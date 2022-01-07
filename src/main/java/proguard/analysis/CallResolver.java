/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.analysis;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.CallResolver.Metrics.MetricType;
import proguard.analysis.datastructure.CodeLocation;
import proguard.analysis.datastructure.Location;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.CallGraph;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.analysis.datastructure.callgraph.SymbolicCall;
import proguard.backport.LambdaExpression;
import proguard.backport.LambdaExpressionCollector;
import proguard.classfile.*;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.InvokeDynamicConstant;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.LineNumberFinder;
import proguard.classfile.visitor.MultiClassVisitor;
import proguard.evaluation.BasicInvocationUnit;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.InvocationUnit;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.value.*;
import proguard.evaluation.value.ParticularValueFactory.ReferenceValueFactory;
import proguard.util.PartialEvaluatorUtils;

/**
 * Collects all method invocations inside the analyzed methods.
 *
 * <p>All method invocation instructions that appear in the bytecode are inspected, and their
 * actual target method is calculated. Java has several invocation instructions,
 * performing virtual, static, dynamic, interface and special calls. While most of these
 * instructions have a constant operand specifying a method name, the actual method that
 * will be called at runtime depends on multiple factors. Sometimes, e.g. when using
 * virtual calls, the invocation target depends on the specific type of the first parameter
 * on the stack, the so-called <code>this</code> pointer.</p>
 *
 * <p>This call analyzer performs a lookup process that adheres to the Java Virtual Machine
 * specification. Being a static analysis, 100% precision cannot be guaranteed, as the specific
 * type of variables at a specific program point is not always known in advance. But using
 * the {@link PartialEvaluator} in combination with intraprocedural possible type analysis of
 * {@link MultiTypedReferenceValue} objects, the resulting call graph should be a superset of
 * the actual calls happening at runtime. This makes it a complete but potentially unsound analysis.</p>
 *
 * <p>In addition to resolving the call target, this analyzer also reconstructs the corresponding
 * arguments and the return value. All of the collected information is wrapped in a {@link Call}
 * object and passed to subscribed {@link CallVisitor}s.</p>
 *
 * @author Samuel Hopstock
 */
public class CallResolver
implements   AttributeVisitor,
             ClassVisitor,
             InstructionVisitor
{

    private static final Logger                                       log                       = LogManager.getLogger(CallResolver.class);
    /**
     * Used in {@link #handleInvokeDynamic(CodeLocation, InvokeDynamicConstant)} to
     * resolve lambda expression invocations.
     */
    private final        Map<InvokeDynamicConstant, LambdaExpression> lambdaExpressionMap       = new HashMap<>();
    private final        LambdaExpressionCollector                    lambdaExpressionCollector = new LambdaExpressionCollector(lambdaExpressionMap);
    /**
     * Used to fill the {@link Call#controlFlowDependent} flag.
     */
    private final        DominatorCalculator                          dominatorCalculator;
    private final        ClassPool                                    programClassPool;
    private final        ClassPool                                    libraryClassPool;
    private final        CallGraph                                    callGraph;
    private final        boolean                                      clearCallValuesAfterVisit;
    private final        boolean                                      useDominatorAnalysis;
    private final        List<CallVisitor>                            visitors;
    /**
     * Calculates concrete values that are created by the bytecode and stored in
     * variables or on the stack. Needed to reconstruct the actual arguments and
     * return value of method calls.
     */
    private final        PartialEvaluator                             particularValueEvaluator;
    private              boolean                                      particularValueEvaluationSuccessful;
    /**
     * Only calculates the type of values on the stack or in variables, but is
     * capable of handling cases where this type may be different depending
     * on the actual control flow path taken during runtime. Needed for
     * resolving all possible call targets of virtual calls that depend
     * on the type of the this pointer during runtime.
     */
    private final        PartialEvaluator                             multiTypeValueEvaluator;
    private              boolean                                      multiTypeEvaluationSuccessful;

    /**
     * Create a new call resolver.
     *
     * @param programClassPool          {@link ClassPool} containing the classes whose
     *                                  calls should be analyzed.
     * @param libraryClassPool          Auxiliary {@link ClassPool} containing framework classes.
     *                                  Their calls are not resolved, but the class structure information
     *                                  (i.e. contained methods) is needed when resolving calls whose
     *                                  target lies in such a library class.
     * @param callGraph                 The {@link CallGraph} to fill with all discovered {@link Call}s.
     * @param clearCallValuesAfterVisit If true, {@link Call#clearValues()} will be called after
     *                                  {@link CallVisitor#visitCall(Call)}. This makes it possible
     *                                  to analyze arguments and the return value of calls while still
     *                                  adding them to a {@link CallGraph} afterwards, as call graph analysis
     *                                  itself usually only requires the call locations and their targets,
     *                                  not the arguments or return value.
     * @param useDominatorAnalysis      If true, a dominator analysis is carried out
     *                                  using the {@link DominatorCalculator} for each
     *                                  method, in order to be able to fill the
     *                                  {@link Call#controlFlowDependent} flag.
     * @param evaluateAllCode           See {@link PartialEvaluator.Builder#setEvaluateAllCode(boolean)}.
     * @param includeSubClasses         If true, virtual calls on class fields, parameters and return values of other methods
     *                                  will take all possible subclasses into account.
     *                                  This is necessary for a more complete
     *                                  call graph, because the runtime type of these objects is not controlled by the current
     *                                  method. E.g. a method that declares its return type to be of type A might also return
     *                                  an object of type B in case B extends A. The same is true for class fields and parameters,
     *                                  so in order to really find all potential calls, this circumstance needs to be modeled.
     *                                  For objects of declared type {@link java.lang.Object} this will be skipped, as the fact
     *                                  that every single Java class is a subclass of object would lead to an immense blow-up
     *                                  of the call graph.
     * @param maxPartialEvaluations     See {@link PartialEvaluator.Builder#stopAnalysisAfterNEvaluations(int)}.
     * @param visitors                  {@link CallVisitor}s that are interested in the
     *                                  results of this analysis.
     */
    public CallResolver(ClassPool programClassPool,
                        ClassPool libraryClassPool,
                        CallGraph callGraph,
                        boolean clearCallValuesAfterVisit,
                        boolean useDominatorAnalysis,
                        boolean evaluateAllCode,
                        boolean includeSubClasses,
                        int maxPartialEvaluations,
                        CallVisitor... visitors)
    {
        this.programClassPool          = programClassPool;
        this.libraryClassPool          = libraryClassPool;
        this.callGraph                 = callGraph;
        this.clearCallValuesAfterVisit = clearCallValuesAfterVisit;
        this.useDominatorAnalysis      = useDominatorAnalysis;
        this.visitors                  = Arrays.asList(visitors);
        dominatorCalculator            = new DominatorCalculator();

        // Initialize the multitype evaluator.
        ValueFactory multiTypeValueFactory = includeSubClasses ?
            new MultiTypedReferenceValueFactory(true, this.programClassPool, this.libraryClassPool) :
            new MultiTypedReferenceValueFactory();
        InvocationUnit multiTypeValueInvocationUnit = new BasicInvocationUnit(multiTypeValueFactory);
        multiTypeValueEvaluator                     = PartialEvaluator.Builder.create()
                                                                              .setValueFactory(multiTypeValueFactory)
                                                                              .setInvocationUnit(multiTypeValueInvocationUnit)
                                                                              .setEvaluateAllCode(evaluateAllCode)
                                                                              .stopAnalysisAfterNEvaluations(maxPartialEvaluations)
                                                                              .build();

        // Initialize the particular value evaluator
        ValueFactory particularValueFactory          = new ParticularValueFactory(new ArrayReferenceValueFactory(),
                                                                                  new ReferenceValueFactory());
        InvocationUnit particularValueInvocationUnit = new ExecutingInvocationUnit(particularValueFactory);
        particularValueEvaluator                     = PartialEvaluator.Builder.create()
                                                                               .setValueFactory(particularValueFactory)
                                                                               .setInvocationUnit(particularValueInvocationUnit)
                                                                               .setEvaluateAllCode(evaluateAllCode)
                                                                               .stopAnalysisAfterNEvaluations(maxPartialEvaluations)
                                                                               .build();
    }

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        // Only interested in program classes.
    }

    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        lambdaExpressionMap.clear();
        programClass.accept(new MultiClassVisitor(lambdaExpressionCollector,
                                                  new AllAttributeVisitor(true, this)));
    }

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
        // Only interested in code attributes.
    }

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Exceptions while executing the partial evaluators are fine, the virtual
        // call resolving and argument/return value reconstruction handle these
        // cases gracefully.
        try
        {
            // Evaluate the code.
            multiTypeEvaluationSuccessful = false;
            multiTypeValueEvaluator.visitCodeAttribute0(clazz, method, codeAttribute);
            multiTypeEvaluationSuccessful = true;
        }
        catch (Exception e)
        {
            Metrics.increaseCount(MetricType.PARTIAL_EVALUATOR_EXCEPTION);
            log.debug("Exception during evaluating types", e);
        }

        try
        {
            // Evaluate the code.
            particularValueEvaluationSuccessful = false;
            particularValueEvaluator.visitCodeAttribute0(clazz, method, codeAttribute);
            particularValueEvaluationSuccessful = true;
        }
        catch (Exception e)
        {
            Metrics.increaseCount(MetricType.PARTIAL_EVALUATOR_EXCEPTION);
            log.debug("Exception during evaluating values", e);
        }

        if (useDominatorAnalysis)
        {
            dominatorCalculator.visitCodeAttribute(clazz, method, codeAttribute);
        }

        codeAttribute.instructionsAccept(clazz, method, this);
    }

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        // Only interested in ConstantInstructions
    }

    @Override
    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        // Get the line number.
        LineNumberFinder lineNumberFinder = new LineNumberFinder(offset);
        codeAttribute.attributesAccept(clazz, method, lineNumberFinder);
        CodeLocation location = new CodeLocation(clazz, method, offset, lineNumberFinder.lineNumber);

        Constant constant = ((ProgramClass) clazz).getConstant(constantInstruction.constantIndex);
        if (constantInstruction.opcode == Instruction.OP_INVOKEDYNAMIC
            && constant instanceof InvokeDynamicConstant)
        {
            handleInvokeDynamic(location, (InvokeDynamicConstant) constant);
        }
        else if (constant instanceof AnyMethodrefConstant)
        {
            AnyMethodrefConstant ref = (AnyMethodrefConstant) constant;
            switch (constantInstruction.opcode)
            {
                case Instruction.OP_INVOKESTATIC:
                    handleInvokeStatic(location, (AnyMethodrefConstant) constant);
                    break;
                case Instruction.OP_INVOKEVIRTUAL:
                case Instruction.OP_INVOKEINTERFACE:
                    handleVirtualMethods(location, ref, constantInstruction.opcode);
                    break;
                case Instruction.OP_INVOKESPECIAL:
                    handleInvokeSpecial(location, ref);
                    break;
                default:
                    Metrics.increaseCount(MetricType.UNSUPPORTED_OPCODE);
                    log.warn("Unsupported invocation opcode " + constantInstruction.opcode + " at " + location);
            }
        }
    }

    private void addCall(CodeLocation location,
                         String targetClass,
                         String targetMethod,
                         String targetDescriptor,
                         int throwsNullptr,
                         byte invocationOpcode,
                         boolean runtimeTypeDependent)
    {
        boolean alwaysInvoked = true;
        if (useDominatorAnalysis)
        {
            alwaysInvoked = dominatorCalculator.dominates(location.offset, DominatorCalculator.EXIT_NODE_OFFSET);
        }
        Call call = instantiateCall(location,
                                    targetClass,
                                    targetMethod,
                                    targetDescriptor,
                                    throwsNullptr,
                                    invocationOpcode,
                                    !alwaysInvoked,
                                    runtimeTypeDependent);
        initArgumentsAndReturnValue(call);

        visitors.forEach(d -> d.visitCall(call));
        if (clearCallValuesAfterVisit)
        {
            call.clearValues();
        }
        if (callGraph != null)
        {
            callGraph.addCall(call);
        }
    }

    /**
     * Creates the appropriate object for the requested call
     * ({@link ConcreteCall} in case the target method is already
     * present in the class pool, otherwise a {@link SymbolicCall}).
     */
    private Call instantiateCall(CodeLocation location,
                                 String targetClass,
                                 String targetMethod,
                                 String targetDescriptor,
                                 int throwsNullptr,
                                 byte invocationOpcode,
                                 boolean controlFlowDependent,
                                 boolean runtimeTypeDependent)
    {
        Call call;
        Clazz containingClass = programClassPool.getClass(targetClass);
        if (containingClass == null)
        {
            containingClass = libraryClassPool.getClass(targetClass);
        }
        if (containingClass == null)
        {
            call = new SymbolicCall(location,
                                    new MethodSignature(targetClass, targetMethod, targetDescriptor),
                                    throwsNullptr,
                                    invocationOpcode,
                                    controlFlowDependent,
                                    runtimeTypeDependent);
            Metrics.increaseCount(MetricType.SYMBOLIC_CALL);
        }
        else
        {
            Method method = containingClass.findMethod(targetMethod, targetDescriptor);
            if (method == null)
            {
                call = new SymbolicCall(location,
                                        new MethodSignature(targetClass, targetMethod, targetDescriptor),
                                        throwsNullptr,
                                        invocationOpcode,
                                        controlFlowDependent,
                                        runtimeTypeDependent);
                Metrics.increaseCount(MetricType.SYMBOLIC_CALL);
            }
            else
            {
                call = new ConcreteCall(location,
                                        containingClass,
                                        method,
                                        throwsNullptr,
                                        invocationOpcode,
                                        controlFlowDependent,
                                        runtimeTypeDependent);
                Metrics.increaseCount(MetricType.CONCRETE_CALL);
                if ((method.getAccessFlags() & AccessConstants.ABSTRACT) != 0)
                {
                    Metrics.increaseCount(MetricType.CALL_TO_ABSTRACT_METHOD);
                }
            }
        }
        return call;
    }

    private void initArgumentsAndReturnValue(Call call)
    {
        boolean         isStaticCall = call.invocationOpcode == Instruction.OP_INVOKESTATIC || call.invocationOpcode == Instruction.OP_INVOKEDYNAMIC;
        MethodSignature target       = call.getTarget();
        List<Value>     arguments    = getArguments(call.caller, target, isStaticCall);
        if (!isStaticCall && !arguments.isEmpty())
        {
            // Handle the instance pointer separately.
            call.setInstance(arguments.remove(0));
        }

        call.setArguments(arguments);

        if (!target.descriptor.getPrettyReturnType().equals("void") && particularValueEvaluationSuccessful)
        {
            call.setReturnValue(PartialEvaluatorUtils.getStackValue(particularValueEvaluator.getStackAfter(call.caller.offset), 0));
        }
    }

    private List<Value> getArguments(CodeLocation location, MethodSignature invokedMethodSig, boolean isStaticCall)
    {
        if (!(multiTypeEvaluationSuccessful && particularValueEvaluationSuccessful))
        {
            return Collections.emptyList();
        }
        if (invokedMethodSig.descriptor.argumentTypes == null)
        {
            log.error("Argument types list of {} is null!", invokedMethodSig);
            return Collections.emptyList();
        }

        List<Value> args = new ArrayList<>();
        int stackOffset  = 0;
        for (int argNumber = invokedMethodSig.descriptor.argumentTypes.size() - 1; argNumber >= 0; argNumber--)
        {
            String argType = invokedMethodSig.descriptor.argumentTypes.get(argNumber);
            // Usually we are interested in concrete values for the arguments, i.e. we take them
            // from the particular value evaluator. But it can happen that this evaluator doesn't
            // know the argument value because it depends on some control flow specifics. Still,
            // we might at least know what type(s) the argument can have, which is better than nothing.
            // In that case the multitype evaluator needs to be consulted. Thus, we first ask the
            // multitype evaluator if the argument is known to have more than one possible type.
            // If this is the case, we can already assume that there is no known particular value
            // for it. Otherwise, we get the particular value as initially planned.
            Value stackTop = PartialEvaluatorUtils.getStackBefore(multiTypeValueEvaluator, location.offset, stackOffset);
            if (!(stackTop instanceof MultiTypedReferenceValue
                  && ((MultiTypedReferenceValue) stackTop).getPotentialTypes().size() > 1))
            {
                stackTop = PartialEvaluatorUtils.getStackBefore(particularValueEvaluator, location.offset, stackOffset);
            }
            // Make sure to reverse the parameter ordering.
            args.add(0, stackTop);
            stackOffset += ClassUtil.internalTypeSize(argType);
        }

        if (!isStaticCall)
        {
            // For virtual calls we have the instance pointer as a first argument.
            Value instance = PartialEvaluatorUtils.getStackBefore(multiTypeValueEvaluator, location.offset, stackOffset);
            if (!(instance instanceof MultiTypedReferenceValue
                  && ((MultiTypedReferenceValue) instance).getPotentialTypes().size() > 1))
            {
                instance = PartialEvaluatorUtils.getStackBefore(particularValueEvaluator, location.offset, stackOffset);
            }
            args.add(0, instance);
        }

        return args;
    }

    /**
     * Resolve <code>invokedynamic</code> instructions. See
     * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokedynamic">JVM spec §6.5.invokedynamic</a>.
     * In Java, this kind of invocation is used for lambda expressions, thus this handler depends on {@link
     * LambdaExpressionCollector} having run beforehand.
     *
     * @param location The {@link Location} of the instruction.
     * @param constant The {@link InvokeDynamicConstant} (JVM spec: "symbolic reference R")
     *                 containing the index to the corresponding bootstrap method that
     *                 identifies the actual call target.
     */
    private void handleInvokeDynamic(CodeLocation location, InvokeDynamicConstant constant)
    {
        if (lambdaExpressionMap.containsKey(constant))
        {
            LambdaExpression target = lambdaExpressionMap.get(constant);
            addCall(location,
                    target.invokedClassName,
                    target.invokedMethodName,
                    target.invokedMethodDesc,
                    Value.NEVER,
                    Instruction.OP_INVOKEDYNAMIC,
                    false
            );
        }
        else
        {
            log.debug("invokedynamic without matching lambda expression at {}", location);
        }
    }

    /**
     * Resolve <code>invokestatic</code> instructions. See <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokestatic">JVM spec §6.5.invokestatic</a>
     *
     * @param location The {@link Location} of the instruction.
     * @param constant The {@link AnyMethodrefConstant} specifying the exact method to be invoked.
     */
    private void handleInvokeStatic(CodeLocation location, AnyMethodrefConstant constant)
    {
        addCall(location,
                constant.getClassName(location.clazz),
                constant.getName(location.clazz),
                constant.getType(location.clazz),
                Value.NEVER,
                Instruction.OP_INVOKESTATIC,
                false
        );
    }

    /**
     * Resolve <code>invokespecial</code> instructions. They are always used for <code>super.x()</code> calls
     * and constructor invocations. According to the
     * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokespecial">JVM spec §6.5.invokespecial</a>,
     * this opcode is also sometimes used for private method calls, but so far I haven't seen that in the wild.
     *
     * @param location The {@link Location} of the instruction.
     * @param ref      The {@link AnyMethodrefConstant} specifying name and descriptor
     *                 of the method to be invoked.
     */
    private void handleInvokeSpecial(CodeLocation location, AnyMethodrefConstant ref)
    {
        Set<String> targets = resolveInvokeSpecial(location.clazz, ref);
        if (targets.isEmpty())
        {
            Metrics.increaseCount(MetricType.MISSING_METHODS);
            log.debug("Missing method {}", ref.getClassName(location.clazz));
        }
        else
        {
            String name       = ref.getName(location.clazz);
            String descriptor = ref.getType(location.clazz);
            for (String target : targets)
            {
                addCall(location,
                        target,
                        name,
                        descriptor,
                        Value.NEVER,
                        Instruction.OP_INVOKESPECIAL,
                        false
                );
            }
        }
    }

    /**
     * The <code>invokespecial</code> resolution algorithm, annotated with JVM spec citations where appropriate,
     * so that the specified lookup process can easily be compared to this
     * implementation.
     *
     * @param callingClass JVM spec: "current class".
     * @param ref          The {@link AnyMethodrefConstant} specifying name
     *                     and descriptor of the method to be invoked.
     * @return The fully qualified names of potential call target classes
     *     (usually just one, but see {@link #resolveFromSuperinterfaces(Clazz, String, String)}
     *     for details on when there might be multiple).
     */
    private Set<String> resolveInvokeSpecial(Clazz callingClass, AnyMethodrefConstant ref)
    {
        String name       = ref.getName(callingClass);
        String descriptor = ref.getType(callingClass);

        // "If all of the following are true, let C be the direct superclass of the current class:"
        Clazz c;
        // ACC_SUPER flag is set (should always be the case, legacy flag). See JVM Spec §4.1.
        if ((callingClass.getAccessFlags() & AccessConstants.SUPER) != 0
            && !name.equals(ClassConstants.METHOD_NAME_INIT) // Not an instance initialization method.
            && ref.referencedClass != null // Referenced class available.
            && (ref.referencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0 // Not an interface reference.
            && callingClass.extends_(ref.referencedClass)
            && !callingClass.getName().equals(ref.referencedClass.getName())) // Referenced class is strictly a superclass of the current class.
        {
            c = callingClass.getSuperClass();
        }
        else
        {
            // "Otherwise, let C be the class or interface named by the symbolic reference".
            c = ref.referencedClass;
        }
        if (c == null)
        {
            // In this case, we don't have the referenced class in our class pool, so we can't be more specific here.
            String className = ref.getClassName(callingClass);
            Metrics.increaseCount(MetricType.MISSING_CLASS);
            log.debug("Missing class {}", className);
            return Collections.singleton(className);
        }

        // 1. (lookup in C directly)
        if (c.findMethod(name, descriptor) != null)
        {
            return Collections.singleton(c.getName());
        }

        Optional<String> target = Optional.empty();
        if ((c.getAccessFlags() &
             AccessConstants.INTERFACE) == 0)
        {
            // 2. (C is a class -> check superclasses transitively)
            target = resolveFromSuperclasses(c, name, descriptor);
        }
        else
        {
            // 3. (C is an interface -> Check if java.lang.Object has a suitable method)
            for (java.lang.reflect.Method m : Object.class.getMethods())
            {
                if ((m.getModifiers() & Modifier.PUBLIC) != 0
                    && m.getName().equals(name)
                    && getDescriptor(m).equals(descriptor))
                {
                    target = Optional.of(ClassConstants.NAME_JAVA_LANG_OBJECT);
                    break;
                }
            }
        }
        // 4. (Otherwise find maximally specific method from superinterfaces)
        return target.map(Collections::singleton)
                     .orElseGet(() -> resolveFromSuperinterfaces(c, name, descriptor));
    }

    /**
     * Get the Descriptor of a {@link java.lang.reflect.Method}.
     */
    public static String getDescriptor(java.lang.reflect.Method m)
    {
        List<String> parameters = Arrays.stream(m.getParameterTypes())
                                        .map(Class::getName)
                                        .collect(Collectors.toList());
        return ClassUtil.internalMethodDescriptor(m.getReturnType().getName(), parameters);
    }

    /**
     * Handle <code>invokevirtual</code> and <code>invokeinterface</code> instructions,
     * as they use more or less the same lookup process.
     * They are used for "normal" method calls, i.e. any instance method.
     * The actual invocation target depends on the type of the <code>this</code> pointer during runtime (first/bottom stack parameter).
     * In order to get a good estimate of this type, the lookup process depends on the analysis by a
     * {@link PartialEvaluator} that yields {@link MultiTypedReferenceValue} elements.
     *
     * @param location The {@link Location} of this instruction.
     * @param ref      The {@link AnyMethodrefConstant} specifying name
     *                 and descriptor of the method to be invoked.
     */
    private void handleVirtualMethods(CodeLocation location, AnyMethodrefConstant ref, byte invocationOpcode)
    {
        String name       = ref.getName(location.clazz);
        String descriptor = ref.getType(location.clazz);

        // Estimate the runtime type of the this pointer: Intraprocedural analysis performed by the partial evaluator.
        int   argumentCount = ClassUtil.internalMethodParameterSize(descriptor, false);
        Value thisPtr       = PartialEvaluatorUtils.getStackBefore(multiTypeValueEvaluator, location.offset, argumentCount - 1);
        if (!(thisPtr instanceof MultiTypedReferenceValue))
        {
            // If the partial evaluation has not finished, this is to be expected and does not warrant an error message.
            if (multiTypeEvaluationSuccessful)
            {
                String classInfo = (thisPtr == null ? "null" : thisPtr.getClass().toString());
                Metrics.increaseCount(MetricType.PARTIAL_EVALUATOR_VALUE_IMPRECISE);
                log.debug("Virtual call at {}: this-pointer is not a multi typed reference value but {}", location, classInfo);
            }
        }
        else
        {
            MultiTypedReferenceValue multiTypeThisPtr = (MultiTypedReferenceValue) thisPtr;

            for (TypedReferenceValue possibleType : multiTypeThisPtr.getPotentialTypes())
            {
                if (possibleType.isNull() == Value.ALWAYS)
                {
                    // This will always throw a NullPointerException, but we still want this info in the call graph.
                    addCall(location,
                            ref.getClassName(location.clazz),
                            ref.getName(location.clazz),
                            ref.getType(location.clazz),
                            Value.ALWAYS,
                            invocationOpcode,
                            multiTypeThisPtr.getPotentialTypes().size() > 1
                    );
                    continue;
                }

                Clazz referencedClass;
                if (ClassUtil.isInternalArrayType(possibleType.getType()))
                {
                    // If anybody wants to call methods on arrays, we need to check java.lang.Object.
                    referencedClass = libraryClassPool.getClass(ClassConstants.NAME_JAVA_LANG_OBJECT);
                }
                else
                {
                    referencedClass = possibleType.getReferencedClass();
                    // Sometimes the type doesn't have a reference to the class yet.
                    // In this case we should try to look it up manually in both class pools.
                    if (referencedClass == null)
                    {
                        referencedClass = programClassPool.getClass(possibleType.getType());
                    }
                    if (referencedClass == null)
                    {
                        referencedClass = libraryClassPool.getClass(possibleType.getType());
                    }
                }

                if (referencedClass == null)
                {
                    // Class still wasn't found, add it to the missing classes.
                    Metrics.increaseCount(MetricType.MISSING_CLASS);
                    log.info("Missing class {}", possibleType.getType());
                }

                Set<String> targetClasses = resolveVirtual(location.clazz, referencedClass, ref);
                if (targetClasses.isEmpty())
                {
                    if (referencedClass != null)
                    {
                        Metrics.increaseCount(MetricType.MISSING_METHODS);
                        log.debug("Missing method {}", ref.getClassName(location.clazz));
                    }
                    targetClasses = Collections.singleton(possibleType.getType());
                }

                for (String targetClass : targetClasses)
                {
                    addCall(location,
                            targetClass,
                            name,
                            descriptor,
                            multiTypeThisPtr.isNull(),
                            invocationOpcode,
                            multiTypeThisPtr.getPotentialTypes().size() > 1
                    );
                }
            }
        }
    }

    /**
     * The <code>invokevirtual</code> and <code>invokeinterface</code> resolution algorithm, annotated with
     * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.invokevirtual">JVM
     * spec §6.5.invokevirtual</a> citations where appropriate, so that the specified lookup
     * process can easily be compared to this implementation.
     *
     * @param callingClass JVM spec: "current class".
     * @param thisPtrType  The type of the <code>this</code> pointer of the
     *                     call (JVM spec: "objectref").
     * @param ref          The {@link AnyMethodrefConstant} specifying name
     *                     and descriptor of the method to be invoked.
     * @return The fully qualified names of potential call target clases
     *     (usually just one, but see {@link #resolveFromSuperinterfaces(Clazz, String, String)}
     *     for details on when there might be multiple).
     */
    private Set<String> resolveVirtual(Clazz callingClass, Clazz thisPtrType, AnyMethodrefConstant ref)
    {
        if (thisPtrType == null)
        {
            return Collections.emptySet();
        }
        String name       = ref.getName(callingClass);
        String descriptor = ref.getType(callingClass);

        // 1. + 2. (Search the class belonging to the this pointer type and all its transitive superclasses)
        return resolveFromSuperclasses(thisPtrType, name, descriptor)
            .map(Collections::singleton)
            // 3. (Otherwise find maximally specific method from superinterfaces)
            .orElseGet(() -> resolveFromSuperinterfaces(thisPtrType, name, descriptor));
    }

    /**
     * Search for the invocation target in a specific class and recursively in all superclasses.
     *
     * @param start      The {@link Clazz} where the lookup is to be started.
     * @param name       The name of the method.
     * @param descriptor The method descriptor.
     * @return An {@link Optional} with the fully qualified name of the class
     *     containing the target method, empty if it couldn't be found.
     */
    private Optional<String> resolveFromSuperclasses(Clazz start, String name, String descriptor)
    {
        Clazz curr = start;
        while (curr != null)
        {
            Method targetMethod = curr.findMethod(name, descriptor);
            if (targetMethod != null && (targetMethod.getAccessFlags() & AccessConstants.ABSTRACT) == 0)
            {
                return Optional.of(curr.getName());
            }

            curr = curr.getSuperClass();
        }
        return Optional.empty();
    }

    /**
     * Search for a maximally specific default implementation in all superinterfaces of a class.
     * This step is potentially unintuitive and difficult to grasp, see
     * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-5.html#jvms-5.4.3.3">JVM spec §5.4.3.3</a>
     * for more information, as well as this great
     * <a href="https://jvilk.com/blog/java-8-wtf-ambiguous-method-lookup/">blog post</a>
     * concerning the resolution pitfalls. The following is based on the information on those
     * websites.
     *
     * @param start      The {@link Clazz} whose superinterfaces are to be searched.
     * @param name       The target method name.
     * @param descriptor The target method descriptor.
     * @return The fully qualified name of the class(es) that contain the method to be invoked.
     *     Be aware that purely from a JVM point of view, this choice can be ambiguous,
     *     in which case it just chooses the candidate randomly.
     *     Here, we don't want to gamble, but rather want to add call graph edges for every possibility,
     *     if this ever happens. Javac ensures that such a case never occurs,
     *     but who knows how the bytecode has been generated, so this possibility is implemented just in case.
     */
    private Set<String> resolveFromSuperinterfaces(Clazz start, String name, String descriptor)
    {
        Set<Clazz> superInterfaces = new HashSet<>();
        getSuperinterfaces(start, superInterfaces);
        // Get all transitive superinterfaces that have a matching method.
        Set<Clazz> applicableInterfaces = superInterfaces.stream()
                                                         .filter(i ->
                                                                 {
                                                                     Method m = i.findMethod(name, descriptor);
                                                                     return m != null && (m.getAccessFlags() & (AccessConstants.PRIVATE
                                                                                                                | AccessConstants.STATIC
                                                                                                                | AccessConstants.ABSTRACT)) == 0;
                                                                 })
                                                         .collect(Collectors.toSet());

        // Tricky part: Find the "maximally specific" implementation,
        // i.e. the lowest applicable interface in the type hierarchy.
        for (Clazz iface : new HashSet<>(applicableInterfaces))
        {
            superInterfaces.clear();
            getSuperinterfaces(iface, superInterfaces);
            // If an applicable interface overrides another applicable interface, it is more specific than the
            // one being overridden -> the overridden interface is no longer applicable.
            superInterfaces.forEach(applicableInterfaces::remove);
        }

        return applicableInterfaces.stream().map(Clazz::getName).collect(Collectors.toSet());
    }

    /**
     * Get the transitive superinterfaces of a class/interface recursively.
     *
     * @param start       The {@link Clazz} where the collection process is to be started.
     * @param accumulator The current set of superinterfaces, so that only one set is constructed at runtime.
     */
    private void getSuperinterfaces(Clazz start, Set<Clazz> accumulator)
    {
        for (int i = 0; i < start.getInterfaceCount(); i++)
        {
            Clazz iface = start.getInterface(i);
            if (iface == null)
            {
                Metrics.increaseCount(MetricType.MISSING_CLASS);
                log.info("Missing class {}", start.getName());
                continue;
            }
            accumulator.add(iface);
            getSuperinterfaces(iface, accumulator);
        }
        if (start.getSuperClass() != null)
        {
            getSuperinterfaces(start.getSuperClass(), accumulator);
        }
    }

    public static class Builder
    {

        private final ClassPool     programClassPool;
        private final ClassPool     libraryClassPool;
        private final CallGraph     callGraph;
        private final CallVisitor[] visitors;
        private       boolean       clearCallValuesAfterVisit = true;
        private       boolean       useDominatorAnalysis      = false;
        private       boolean       evaluateAllCode           = false;
        private       boolean       includeSubClasses         = false;
        private       int           maxPartialEvaluations     = 50;

        public Builder(ClassPool programClassPool, ClassPool libraryClassPool, CallGraph callGraph, CallVisitor... visitors)
        {
            this.programClassPool = programClassPool;
            this.libraryClassPool = libraryClassPool;
            this.callGraph = callGraph;
            this.visitors = visitors;
        }

        /**
         * If true, {@link Call#clearValues()} will be called after
         * {@link CallVisitor#visitCall(Call)}. This makes it possible
         * to analyze arguments and the return value of calls while still
         * adding them to a {@link CallGraph} afterwards, as call graph analysis
         * itself usually only requires the call locations and their targets,
         * not the arguments or return value.
         */
        public Builder setClearCallValuesAfterVisit(boolean clearCallValuesAfterVisit)
        {
            this.clearCallValuesAfterVisit = clearCallValuesAfterVisit;
            return this;
        }

        /**
         * If true, a dominator analysis is carried out using the {@link DominatorCalculator}
         * for each method, in order to be able to fill the {@link Call#controlFlowDependent} flag.
         */
        public Builder setUseDominatorAnalysis(boolean useDominatorAnalysis)
        {
            this.useDominatorAnalysis = useDominatorAnalysis;
            return this;
        }

        /**
         * See {@link PartialEvaluator.Builder#setEvaluateAllCode(boolean)}.
         */
        public Builder setEvaluateAllCode(boolean evaluateAllCode)
        {
            this.evaluateAllCode = evaluateAllCode;
            return this;
        }

        /**
         * If true, virtual calls on class fields, parameters and return values of other methods
         * will take all possible subclasses into account.
         *
         * <p>This is necessary for a more complete
         * call graph, because the runtime type of these objects is not controlled by the current
         * method. E.g. a method that declares its return type to be of type A might also return
         * an object of type B in case B extends A. The same is true for class fields and parameters,
         * so in order to really find all potential calls, this circumstance needs to be modeled.
         * For objects of declared type {@link java.lang.Object} this will be skipped, as the fact
         * that every single Java class is a subclass of object would lead to an immense blow-up
         * of the call graph.</p>
         */
        public Builder setIncludeSubClasses(boolean includeSubClasses)
        {
            this.includeSubClasses = includeSubClasses;
            return this;
        }

        /**
         * See {@link PartialEvaluator.Builder#stopAnalysisAfterNEvaluations(int)}.
         */
        public Builder setMaxPartialEvaluations(int maxPartialEvaluations)
        {
            this.maxPartialEvaluations = maxPartialEvaluations;
            return this;
        }

        public CallResolver build()
        {
            return new CallResolver(programClassPool,
                                    libraryClassPool,
                                    callGraph,
                                    clearCallValuesAfterVisit,
                                    useDominatorAnalysis,
                                    evaluateAllCode,
                                    includeSubClasses,
                                    maxPartialEvaluations,
                                    visitors);

        }
    }

    /**
     * Utility to collect statistical information about the call resolution process.
     */
    public static class Metrics
    {

        /**
         * Constants which are used as metric types.
         */
        public enum MetricType
        {
            MISSING_CLASS,
            MISSING_METHODS,
            UNSUPPORTED_OPCODE,
            PARTIAL_EVALUATOR_EXCEPTION,
            PARTIAL_EVALUATOR_VALUE_IMPRECISE,
            SYMBOLIC_CALL,
            CONCRETE_CALL,
            CALL_TO_ABSTRACT_METHOD
        }

        public static final Map<MetricType, Integer> counts = new TreeMap<>();

        public static void increaseCount(MetricType type)
        {
            counts.merge(type, 1, Integer::sum);
        }

        /**
         * Get all collected data as a string and clear it afterwards.
         */
        public static String flush()
        {
            StringBuilder result = new StringBuilder("Call resolver Metrics:\n");

            counts.forEach((type, count) -> result.append(type.name())
                                                  .append(": ")
                                                  .append(count)
                                                  .append("\n"));
            counts.clear();
            return result.toString();
        }
    }
}
