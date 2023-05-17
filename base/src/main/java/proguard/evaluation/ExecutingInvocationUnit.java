/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

package proguard.evaluation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.ConstantValueAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.DoubleConstant;
import proguard.classfile.constant.FieldrefConstant;
import proguard.classfile.constant.FloatConstant;
import proguard.classfile.constant.IntegerConstant;
import proguard.classfile.constant.LongConstant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.MemberAccessFilter;
import proguard.classfile.visitor.MemberVisitor;
import proguard.classfile.visitor.ReturnClassExtractor;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ParticularReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.ReflectiveMethodCallUtil;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static proguard.classfile.AccessConstants.FINAL;
import static proguard.classfile.AccessConstants.STATIC;
import static proguard.classfile.ClassConstants.METHOD_NAME_INIT;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING_BUFFER;
import static proguard.classfile.ClassConstants.NAME_JAVA_LANG_STRING_BUILDER;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;
import static proguard.classfile.TypeConstants.BOOLEAN;
import static proguard.classfile.TypeConstants.BYTE;
import static proguard.classfile.TypeConstants.CHAR;
import static proguard.classfile.TypeConstants.DOUBLE;
import static proguard.classfile.TypeConstants.FLOAT;
import static proguard.classfile.TypeConstants.INT;
import static proguard.classfile.TypeConstants.LONG;
import static proguard.classfile.TypeConstants.SHORT;
import static proguard.classfile.TypeConstants.VOID;
import static proguard.classfile.util.ClassUtil.externalClassName;
import static proguard.classfile.util.ClassUtil.isInternalPrimitiveType;
import static proguard.classfile.util.ClassUtil.isNullOrFinal;
import static proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE;
import static proguard.evaluation.value.ReflectiveMethodCallUtil.callMethod;

/**
 * <p>This {@link ExecutingInvocationUnit} reflectively executes the method calls it visits on a given
 * {@link ParticularReferenceValue}.</p>
 *
 * <p>After the (reflective) execution of method, it also takes care to replace values on stack/variables
 * if needed. This needs to be done, as the PartialEvaluator treats all entries on stack/variables as immutable, and assumes that every change creates a new object.</p>
 *
 * <p>Before a method call, the stack/variables can contain multiple references to the same object. If the
 * method call then creates a new Variable (representing an updated object), we need to update all references to this new Variable.</p>
 *
 * <p>There are some methods which always return a new object when the method is actually executed in the
 * JVM (e.g. StringBuilder.toString). For such method calls, we never update the stack/variables (Denylist, Case 1)</p>
 *
 * <p>For certain methods (e.g. the Constructor), we always need to replace the value on stack/variables
 * (Allowlist, Case 3)</p>
 *
 * <p>In all other cases, we assume the underlying object was changed if the returned value is of the same
 * type as the called upon instance. This is an approximation, which works well for Strings, StringBuffer, StringBuilder (Approximation, Case 3)</p>
 *
 * @author Dennis Titze
 */
public class ExecutingInvocationUnit
    extends BasicInvocationUnit
{

    public static boolean DEBUG = System.getProperty("eiu") != null;

    // Used to store the parameters for one method call.
    private static final Logger                   log = LogManager.getLogger(ExecutingInvocationUnit.class);
    @Nullable
    private              Value[]                  parameters;
    private final        Map<String, Set<String>> alwaysReturnsNewInstance;
    private final        Map<String, Set<String>> alwaysModifiesInstance;
    private final        boolean                  enableSameInstanceIdApproximation;

    /**
     * Creates an invocation unit resolving the methods from the specified classes via reflection.
     *
     * @param valueFactory                      a value factory
     * @param alwaysReturnsNewInstance          a mapping from class name to method name of methods that the invocation unit will assume to always return a new reference
     * @param alwaysModifiesInstance            a mapping from class name to method name of methods that the invocation unit will assume to modify the calling instance
     * @param enableSameInstanceIdApproximation whether the invocation unit will assume for classes not supported for execution that they might return the same reference of the calling
     *                                          instance if their types match
     */
    protected ExecutingInvocationUnit(ValueFactory             valueFactory,
                                      Map<String, Set<String>> alwaysReturnsNewInstance,
                                      Map<String, Set<String>> alwaysModifiesInstance,
                                      boolean                  enableSameInstanceIdApproximation)
    {
        super(valueFactory);
        this.enableSameInstanceIdApproximation = enableSameInstanceIdApproximation;
        this.alwaysReturnsNewInstance = alwaysReturnsNewInstance;
        this.alwaysModifiesInstance = alwaysModifiesInstance;
    }

    @Deprecated
    /**
     * Deprecated constructor, use {@link proguard.evaluation.ExecutingInvocationUnit.Builder}.
     */
    public ExecutingInvocationUnit(ValueFactory valueFactory)
    {
        this(valueFactory,
             Builder.alwaysReturnsNewInstanceDefault(),
             Collections.emptyMap(),
             false);
    }

    /**
     * Builds an {@link ExecutingInvocationUnit}.
     */
    public static class Builder
    {

        protected Map<String, Set<String>> alwaysReturnsNewInstance          = alwaysReturnsNewInstanceDefault();
        protected Map<String, Set<String>> alwaysModifiesInstance            = Collections.emptyMap();
        protected boolean                  enableSameInstanceIdApproximation = false;

        /**
         * @param valueFactory a value factory
         * @return the build {@link ExecutingInvocationUnit}
         */
        public ExecutingInvocationUnit build(ValueFactory valueFactory)
        {
            return new ExecutingInvocationUnit(valueFactory,
                                               alwaysReturnsNewInstance,
                                               alwaysModifiesInstance,
                                               enableSameInstanceIdApproximation);
        }

        /**
         * @param alwaysReturnsNewInstance a mapping from class name to method name of methods that the invocation unit will assume to always return a new reference
         * @return the {@link Builder}
         */
        public Builder setAlwaysReturnsNewInstance(Map<String, Set<String>> alwaysReturnsNewInstance)
        {
            this.alwaysReturnsNewInstance = alwaysReturnsNewInstance;
            return this;
        }

        /**
         * @param alwaysModifiesInstance a mapping from class name to method name of methods that the invocation unit will assume to modify the calling instance
         * @return the {@link Builder}
         */
        public Builder setAlwaysModifiesInstance(Map<String, Set<String>> alwaysModifiesInstance)
        {
            this.alwaysModifiesInstance = alwaysModifiesInstance;
            return this;
        }

        /**
         * @param enableSameInstanceIdApproximation whether the invocation unit will assume for classes not supported for execution that they might return the same reference of the calling
         *                                          instance if their types match
         * @return the {@link Builder}
         */
        public Builder setEnableSameInstanceIdApproximation(boolean enableSameInstanceIdApproximation)
        {
            this.enableSameInstanceIdApproximation = enableSameInstanceIdApproximation;
            return this;
        }

        private static Map<String, Set<String>> alwaysReturnsNewInstanceDefault()
        {
            return new HashMap<String, Set<String>>() {{
                put(NAME_JAVA_LANG_STRING_BUILDER, Collections.singleton("toString"));
                put(NAME_JAVA_LANG_STRING_BUFFER, Collections.singleton("toString"));
                put(NAME_JAVA_LANG_STRING, Stream.of("toString", "valueOf").collect(Collectors.toSet()));
            }};
        }
    }

    // Overrides for BasicInvocationUnit

    @Override
    public void setMethodParameterValue(Clazz                clazz,
                                        AnyMethodrefConstant anyMethodrefConstant,
                                        int                  parameterIndex,
                                        Value                value)
    {
        if (parameters == null)
        {
            // This is the first invocation of setMethodParameterValue for this method.
            // Initialize array
            String type = anyMethodrefConstant.getType(clazz);
            int parameterCount = ClassUtil.internalMethodParameterCount(type, isStatic);
            parameters = new Value[parameterCount];
        }
        parameters[parameterIndex] = value;
    }

    @Override
    public boolean methodMayHaveSideEffects(Clazz clazz,
                                            AnyMethodrefConstant anyMethodrefConstant,
                                            String               returnType)
    {
        // Only execute methods which have at least one parameter.
        // If the method is a static method, this means that at least one parameter is set,
        // if the method is an instance call, at least the instance needs to be set.
        // Static calls without parameters will not be called, as the side-effects of those cannot currently be tracked.
        return parameters != null && parameters.length > 0;
    }

    @Override
    public Value getMethodReturnValue(Clazz                clazz,
                                      AnyMethodrefConstant anyMethodrefConstant,
                                      String               returnType)
    {

        String baseClassName   = anyMethodrefConstant.getClassName(clazz);
        Clazz  referencedClass = getReferencedClass(anyMethodrefConstant, false);

        if (!isSupportedMethodCall(baseClassName, anyMethodrefConstant.getName(clazz)))
        {
            if (enableSameInstanceIdApproximation
                && anyMethodrefConstant.referencedMethod != null
                && !isInternalPrimitiveType(returnType)
                && parameters != null
                && parameters.length > 0
                && parameters[0] instanceof IdentifiedReferenceValue
                && returnsOwnInstance(baseClassName,
                                      anyMethodrefConstant.getName(clazz),
                                      anyMethodrefConstant.referencedMethod.getDescriptor(anyMethodrefConstant.referencedClass),
                                      (anyMethodrefConstant.referencedMethod.getAccessFlags() & STATIC) != 0,
                                      returnType))
            {
                return valueFactory.createReferenceValueForId(returnType,
                                                              referencedClass,
                                                              isNullOrFinal(referencedClass),
                                                              true,
                                                              ((IdentifiedReferenceValue) parameters[0]).id);
            }
            else
            {
                return valueFactory.createValue(returnType, referencedClass, isNullOrFinal(referencedClass), true);
            }
        }

        // TODO: Passing calling context parameters.
        Value reflectedReturnValue = executeMethod(null, null, 0, anyMethodrefConstant.referencedClass, anyMethodrefConstant.referencedMethod, parameters);

        updateStackAndVariables(clazz, anyMethodrefConstant, returnType, reflectedReturnValue);

        // For a void-method, null is expected as return value.
        return returnType.charAt(0) == VOID ? null : reflectedReturnValue;
    }


    @Override
    public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        try
        {
            super.visitAnyMethodrefConstant(clazz, anyMethodrefConstant);
        }
        finally
        {
            this.parameters = null;
        }
    }


    @Override
    public Value getFieldValue(Clazz clazz, FieldrefConstant fieldrefConstant, String type)
    {
        // get value from static final fields
        FieldValueGetterVisitor constantVisitor = new FieldValueGetterVisitor();
        fieldrefConstant.referencedFieldAccept(
            new MemberAccessFilter(STATIC | FINAL, 0, constantVisitor)
        );
        return constantVisitor.value == null ? super.getFieldValue(clazz, fieldrefConstant, type) : constantVisitor.value;
    }

    private class FieldValueGetterVisitor
        implements MemberVisitor,
                   AttributeVisitor,
                   ConstantVisitor
    {

        Value value = null;
        private ProgramField currentField;

        // Implementations for MemberVisitor

        @Override
        public void visitAnyMember(Clazz clazz, Member member)
        {
        }

        @Override
        public void visitProgramField(ProgramClass programClass, ProgramField programField)
        {
            this.currentField = programField;
            programField.attributesAccept(programClass, this);
        }

        // Implementations for AttributeVisitor

        @Override
        public void visitAnyAttribute(Clazz clazz, Attribute attribute)
        {
        }


        @Override
        public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
        {
            clazz.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex, this);
        }

        // Implementations for ConstantVisitor

        @Override
        public void visitAnyConstant(Clazz clazz, Constant constant)
        {
        }

        @Override
        public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
        {
            value = valueFactory.createIntegerValue(integerConstant.getValue());
        }

        @Override
        public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
        {
            value = valueFactory.createFloatValue(floatConstant.getValue());
        }

        @Override
        public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
        {
            value = valueFactory.createDoubleValue(doubleConstant.getValue());
        }

        @Override
        public void visitLongConstant(Clazz clazz, LongConstant longConstant)
        {
            value = valueFactory.createLongValue(longConstant.getValue());
        }

        @Override
        public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
        {
            value = valueFactory.createReferenceValue(TYPE_JAVA_LANG_STRING,
                                                      currentField.referencedClass,
                                                      isNullOrFinal(currentField.referencedClass),
                                                      false,
                                                      stringConstant.getString(clazz));
        }
    }

    /**
     * Returns true if a method call can be called reflectively.
     *
     * Currently, supports all methods of String, StringBuilder, StringBuffer.
     */
    public boolean isSupportedMethodCall(String internalClassName, String methodName)
    {
        return isSupportedClass(internalClassName);
    }

    /**
     * Returns true if the class is supported by the invocation unit.
     */
    public boolean isSupportedClass(String internalClassName)
    {
        switch (internalClassName)
        {
            case NAME_JAVA_LANG_STRING_BUILDER:
            case NAME_JAVA_LANG_STRING_BUFFER:
            case NAME_JAVA_LANG_STRING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Executes a method, by reflectively trying to call the method represented by the given {@link Call}.
     *
     * @param call       The method to call.
     * @param parameters An array containing the parameters values (for a non-static call, parameters[0] is the instance.
     * @return The return value of the method call. Even for a void method, a created value might be returned, which might need to be replaced on stack/values (e.g. for Constructors).
     */
    public Value executeMethod(ConcreteCall call, Value... parameters)
    {
        return executeMethod(
            call.caller.clazz,
            (Method) call.caller.member,
            call.caller.offset,
            call.getTargetClass(),
            call.getTargetMethod(),
            parameters
        );
    }

    /**
     * Executes a method, by reflectively trying to call it with the given parameters.
     *
     * @param callingClass  The class from where the method is being executed from.
     * @param callingMethod The method from where the method is being executed from.
     * @param callingOffset The offset from where the method is being executed from.
     * @param parameters    An array containing the parameters values (for a non-static call, parameters[0] is the instance.
     * @return The return value of the method call. Even for a void method, a created value might be returned, which might need to be replaced on stack/values (e.g. for Constructors).
     */
    public Value executeMethod(Clazz callingClass, Method callingMethod, int callingOffset, Clazz clazz, Method method, Value... parameters)
    {
        if (clazz == null || method == null)
        {
            return UNKNOWN_VALUE;
        }

        boolean isStatic      = (method.getAccessFlags() & STATIC) != 0;
        String  baseClassName = clazz.getName();
        String  methodName    = method.getName(clazz);
        String  descriptor    = method.getDescriptor(clazz);
        String  returnType    = ClassUtil.internalMethodReturnType(descriptor);
        Clazz   returnClazz   = getReferencedClass(clazz, method, methodName.equals(METHOD_NAME_INIT));

        // On error: return at least a typed reference and potentially a replacement
        // for the instance, if we know that the method call should return its own instance
        // according the approximation of `returnsOwnInstance`.
        String  finalReturnType = returnType;
        BiFunction<ReferenceValue, Object, Value> errorHandler = (instance, objectId) ->
        {

            if (objectId != null && returnsOwnInstance(finalReturnType, methodName, descriptor, isStatic, instance == null ? null : instance.internalType()))
            {
                return valueFactory.createReferenceValueForId(finalReturnType, returnClazz, isNullOrFinal(returnClazz), true, objectId);
            }
            else if (isInternalPrimitiveType(finalReturnType))
            {
                return createPrimitiveValue(finalReturnType);
            }
            else
            {
                return valueFactory.createValue(finalReturnType, returnClazz, isNullOrFinal(returnClazz), true);
            }
        };

        ReferenceValue instance = !isStatic && parameters[0] instanceof ReferenceValue ? parameters[0].referenceValue()           : null;
        Object         objectId = instance instanceof IdentifiedReferenceValue         ? ((IdentifiedReferenceValue) instance).id : null;

        if (!isSupportedMethodCall(baseClassName, methodName))
        {
            return errorHandler.apply(instance, objectId);
        }

        if (!isStatic)
        {
            // instance must be at least specific.
            if (instance == null || !instance.isSpecific())
            {
                return errorHandler.apply(instance, objectId);
            }
        }

        // if this is a non-static call, the first parameter is the instance, so all accesses to the arrays need to be offset by 1.
        // if this is a static call, the first parameter is actually the first.
        int paramOffset = isStatic ? 0 : 1;

        // check if any of the parameters is Unknown. If yes, the result is unknown as well.
        for (int i = paramOffset; i < parameters.length; i++)
        {
            if (!parameters[i].isParticular())
            {
                return errorHandler.apply(instance, objectId);
            }
        }

        boolean resultMayBeNull      = true;
        boolean resultMayBeExtension = true;
        Object  callingInstance      = null; // null for static
        Object  methodResult;

        try
        {
            // collect all class types of the parameters (to search for the correct method reflectively).
            Class<?>[] parameterClasses = ReflectiveMethodCallUtil.stringtypesToClasses(descriptor);

            // collect all objects of the parameters.
            Object[] parameterObjects = new Object[parameters.length - paramOffset];
            for (int i = paramOffset; i < parameters.length; i++)
            {
                parameterObjects[i - paramOffset] = ReflectiveMethodCallUtil.getObjectForValue(parameters[i], parameterClasses[i - paramOffset]);
            }

            if (methodName.equals(METHOD_NAME_INIT)) //CTOR
            {
                methodResult = ReflectiveMethodCallUtil.callConstructor(externalClassName(baseClassName),
                                                                        parameterClasses,
                                                                        parameterObjects);

                resultMayBeNull      = false; // the return of the ctor will not be null if it does not throw.
                resultMayBeExtension = false; // the return of the ctor will always be this specific type.
            }
            else // non-constructor method call.
            {
                if (instance != null && instance.isParticular())
                {
                    switch (baseClassName)
                    {
                        // create a copy of the object stored in the ParticularReferenceValue.
                        case NAME_JAVA_LANG_STRING_BUILDER:
                            callingInstance = new StringBuilder((StringBuilder) instance.value());
                            break;
                        case NAME_JAVA_LANG_STRING_BUFFER:
                            callingInstance = new StringBuffer((StringBuffer) instance.value());
                            break;
                        case NAME_JAVA_LANG_STRING:
                            callingInstance = (String)instance.value();
                            break;
                    }
                }

                if (isStatic || callingInstance != null)
                {
                    methodResult = callMethod(ClassUtil.externalClassName(baseClassName), methodName, callingInstance, parameterClasses, parameterObjects);
                }
                else
                {
                    return errorHandler.apply(instance, objectId);
                }
            }
        }
        catch (NullPointerException | InvocationTargetException e)
        {
            // a parameter might be null, and the method might not be able to handle it -> NullPointerException, or
            // an index might be wrongly / not initialized -> StringIndexOutOfBoundsException.
            if (DEBUG)
            {
                System.err.println("Invocation exception during method execution: " + e.getClass().getSimpleName() + ": - " + e.getMessage());
            }
            return errorHandler.apply(instance, objectId);
        }
        catch (RuntimeException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException e)
        {
            if (DEBUG)
            {
                System.err.println("Error reflectively calling " + baseClassName + "." + methodName + descriptor + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
            return errorHandler.apply(instance, objectId);
        }

        // If the return value is a primitive, store this inside its corresponding ParticularValue, e.g. int -> ParticularIntegerValue.
        if (returnType.length() == 1 && isInternalPrimitiveType(returnType))
        {
            return createPrimitiveValue(methodResult, returnType);
        }

        // If it is not a primitiveValue, it will be stored in a ParticularReferenceValue.
        // If the return type is void, we still return an object, since this might need to be replaced on stack/variables.
        // To create a referenceValue with a correct type (i.e. not void), we update the type from the instance we called upon.
        if (!isStatic && returnType.equals("V"))
        {
            returnType = instance.referenceValue().getType();
        }

        // If there ID is already know, use the same ID;
        // unless the instance modifies itself or is different.
        if (objectId != null && (alwaysModifiesInstance(baseClassName, methodName) || callingInstance == methodResult))
        {
            return valueFactory.createReferenceValueForId(returnType,
                                                          // check necessary for primitive arrays, in case the method returns a primitive array its last referenced class will be
                                                          // its last parameter (null correctly just if it has no reference class parameters), since primitive types are not a referenced class
                                                          ClassUtil.isInternalPrimitiveType(ClassUtil.internalTypeFromArrayType(returnType))
                                                              ? null
                                                              : returnClazz,
                                                          resultMayBeExtension,
                                                          resultMayBeNull,
                                                          objectId,
                                                          methodResult);
        }
        else
        {
            return valueFactory.createReferenceValue(returnType,
                                                     // check necessary for primitive arrays, in case the method returns a primitive array its last referenced class will be
                                                     // its last parameter (null correctly just if it has no reference class parameters), since primitive types are not a referenced class
                                                     ClassUtil.isInternalPrimitiveType(ClassUtil.internalTypeFromArrayType(returnType))
                                                         ? null
                                                         : returnClazz,
                                                     resultMayBeExtension,
                                                     resultMayBeNull,
                                                     callingClass,
                                                     callingMethod,
                                                     callingOffset,
                                                     methodResult);
        }
    }

    // private methods

    /**
     * Create a ParticularValue containing the object, using the factory of this instance
     *
     * @param type the type the resulting Value should have.
     * @return the new ParticularValue, or null if the type cannot be converted to a ParticularValue.
     */
    private Value createPrimitiveValue(String type)
    {
        switch (type.charAt(0))
        {
            case BOOLEAN:
            case INT:
            case SHORT:
            case BYTE:
            case CHAR:
                return valueFactory.createIntegerValue();
            case FLOAT:
                return valueFactory.createFloatValue();
            case DOUBLE:
                return valueFactory.createDoubleValue();
            case LONG:
                return valueFactory.createLongValue();
        }
        // unknown type
        return null;
    }

    /**
     * Create a ParticularValue containing the object, using the factory of this instance
     *
     * @param obj  the object to wrap.
     * @param type the type the resulting Value should have.
     * @return the new ParticularValue, or null if the type cannot be converted to a ParticularValue.
     */
    private Value createPrimitiveValue(Object obj, String type)
    {
        switch (type.charAt(0))
        {
            case BOOLEAN:
                return valueFactory.createIntegerValue(((Boolean) obj) ? 1 : 0);
            case CHAR:
                return valueFactory.createIntegerValue((Character) obj);
            case BYTE:
                return valueFactory.createIntegerValue((Byte) obj);
            case SHORT:
                return valueFactory.createIntegerValue((Short) obj);
            case INT:
                return valueFactory.createIntegerValue((Integer) obj);
            case FLOAT:
                return valueFactory.createFloatValue((Float) obj);
            case DOUBLE:
                return valueFactory.createDoubleValue((Double) obj);
            case LONG:
                return valueFactory.createLongValue((Long) obj);
        }
        // unknown type
        return null;
    }

    /**
     * Iterate the variables and replace all occurrences of oldValue with newValue.
     *
     * @param newValue  the value which should be put in.
     * @param oldValue  the value to replace.
     * @param variables the variables to look through.
     */
    private void replaceReferenceInVariables(Value     newValue,
                                             Value     oldValue,
                                             Variables variables)
    {
        if (oldValue.isSpecific())
        {
            // we need to replace all instances on the stack and in the vars with new instances now.
            if (variables != null)
            {
                for (int i = 0; i < variables.size(); i++)
                {
                    Value value = variables.getValue(i);
                    if (Objects.equals(value, oldValue))
                    {
                        variables.store(i, newValue);
                    }
                    if (value != null && value.isCategory2())
                    {
                        i++;
                    }
                }
            }
        }
    }

    /**
     * Iterate the stack and replace all occurrences of oldValue with newValue.
     *
     * @param newValue the value which should be put in.
     * @param oldValue the value to replace.
     * @param stack    the stack to look through.
     */
    private void replaceReferenceOnStack(Value newValue, Value oldValue, Stack stack)
    {
        if (oldValue.isSpecific())
        {
            for (int i = 0; i < stack.size(); i++)
            {
                Value top = stack.getTop(i);
                if (Objects.equals(top, oldValue))
                {
                    stack.setTop(i, newValue);
                }
            }
        }
    }


    /**
     * Returns true, if a call to internalClassName.methodName should return a new (i.e. not-linked) instance.
     *
     * @param internalClassName full class name of the base class of the method (e.g. java/lang/StringBuilder).
     * @param methodName        method name.
     * @return if it should create a new instance.
     */
    public boolean alwaysReturnsNewInstance(String internalClassName, String methodName)
    {
        return alwaysReturnsNewInstance.getOrDefault(internalClassName, new HashSet<>()).contains(methodName);
    }

    /**
     * Returns true, if a call to internalClassName.methodName always modified the called upon instance. The modified value will be returned by the reflective method call
     *
     * @param internalClassName full class name of the base class of the method (e.g. java/lang/StringBuilder).
     * @param methodName        method name.
     * @return if the instance is always modified.
     */
    private boolean alwaysModifiesInstance(String internalClassName, String methodName)
    {
        // The constructor always modifies the instance.
        return methodName.equals(METHOD_NAME_INIT) || alwaysModifiesInstance.getOrDefault(internalClassName, new HashSet<>()).contains(methodName);
    }

    /**
     * Determines if the stack/variables need to be updated.
     * <p>Limitations:
     * We are only checking if the instance of the call was modified, i.e.
     * - for static calls, no value should be replaced on stack/variables (since no instance exists), and
     * - the method call assumes that the parameters are not changed.
     *
     * <p>This is an approximation which works well for StringBuilder/StringBuffer and Strings.
     */
    public boolean returnsOwnInstance(Clazz clazz, Method method, Value instance)
    {
        if (!(instance instanceof ReferenceValue))
        {
            return false;
        }

        String  instanceType      = instance.internalType();
        String  internalClassName = clazz.getName();
        String  methodName        = method.getName(clazz);
        boolean isStatic          = (method.getAccessFlags() & STATIC) != 0;

        return returnsOwnInstance(internalClassName, methodName, method.getDescriptor(clazz), isStatic, instanceType);
    }

    /**
     * Determines if the stack/variables need to be updated.
     * <p>Limitations:
     * We are only checking if the instance of the call was modified, i.e.
     * - for static calls, no value should be replaced on stack/variables (since no instance exists), and
     * - the method call assumes that the parameters are not changed.
     *
     * <p>This is an approximation which works well for StringBuilder/StringBuffer and Strings.
     */
    private boolean returnsOwnInstance(String internalClassName, String methodName, String methodDescriptor, boolean isStatic, String instanceType)
    {
        String returnType = ClassUtil.internalMethodReturnType(methodDescriptor);

        if (isInternalPrimitiveType(returnType))
        {
            return false;
        }

        if (isStatic)
        {
            // For a static method, there is never an instance to update,
            // we do not track internal state, so no static internals of the class are stored.
            return false;
        }

        if (alwaysReturnsNewInstance(internalClassName, methodName))
        {
            // Denylist (Case 1).
            // The value returned by the PartialEvaluator never needs to be replaced.
            return false;
        }

        if (alwaysModifiesInstance(internalClassName, methodName) || // Allowlist (Case 2). The instance always needs to be replaced.
            Objects.equals(returnType, instanceType)) // Approximation (Case 3)
        // For now, we assume that the instance is changed, if the method is not in the Denylist, and the returnType equals
        // the type of the called upon instance. E.g., if StringBuilder.append() returns a StringBuilder, we assume that this
        // is the instance which needs to be replaced.
        {
            return true;
        }

        return false;
    }


    private void updateStackAndVariables(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, String returnType, Value reflectedReturnValue)
    {
        String baseClassName = anyMethodrefConstant.getClassName(clazz);
        String methodName    = anyMethodrefConstant.getName(clazz);

        if (returnsOwnInstance(clazz, anyMethodrefConstant.referencedMethod, parameters.length > 0 ? parameters[0] : null))
        {
            Value updateValue = reflectedReturnValue;

            // If the reflective method call fails, it returned null. We create an unknown value to use as replacement on stack/variables.
            // this can happen e.g. if we have a substring with incorrect lengths, or unknown values in the parameters, etc.
            if (updateValue == null)
            {
                // To create a correct (failed) object, we need to know the type. For void methods, we use the type of the instance
                updateValue = valueFactory.createValue((returnType.charAt(0) == VOID)
                                                           ? ClassUtil.internalTypeFromClassName(baseClassName)
                                                           : returnType,
                                                       getReferencedClass(anyMethodrefConstant, methodName.equals(METHOD_NAME_INIT)),
                                                       true,
                                                       true);
            }

            replaceReferenceInVariables(updateValue, parameters[0], variables);
            replaceReferenceOnStack(updateValue, parameters[0], stack);
        }
    }

    private Clazz getReferencedClass(Clazz clazz, Method method, boolean isCtor)
    {
        if (isCtor)
        {
            return clazz; // this is the class of "this", i.e., the type of this constructor.
        }

        // extract the class from the referenced classes
        ReturnClassExtractor returnClassExtractor = new ReturnClassExtractor();
        method.accept(clazz, returnClassExtractor);
        return returnClassExtractor.returnClass; // can be null
    }

    /**
     * Returns the class of the returnClassPool type, if available, null otherwise.
     * For a Constructor, we always return a type, even if the return type of the method would be void. This is required,
     * since we need to handle constructors differently in general (see Javadoc).
     */
    private Clazz getReferencedClass(AnyMethodrefConstant anyMethodrefConstant, boolean isCtor)
    {
        if (isCtor)
        {
            return anyMethodrefConstant.referencedClass; // this is the class of "this", i.e., the type of this constructor.
        }

        // extract the class from the referenced classes
        ReturnClassExtractor returnClassExtractor = new ReturnClassExtractor();
        anyMethodrefConstant.referencedMethodAccept(returnClassExtractor);
        return returnClassExtractor.returnClass; // can be null
    }
}
