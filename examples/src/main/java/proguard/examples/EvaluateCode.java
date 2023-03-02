package proguard.examples;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.InitializationUtil;
import proguard.classfile.util.WarningPrinter;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.ClassNameFilter;
import proguard.classfile.visitor.MemberNameFilter;
import proguard.evaluation.BasicInvocationUnit;
import proguard.evaluation.InvocationUnit;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.ParticularReferenceValueFactory;
import proguard.evaluation.ReferenceTracingInvocationUnit;
import proguard.evaluation.ReferenceTracingValueFactory;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.evaluation.value.ArrayReferenceValueFactory;
import proguard.evaluation.value.BasicValueFactory;
import proguard.evaluation.value.DetailedArrayValueFactory;
import proguard.evaluation.value.IdentifiedValueFactory;
import proguard.evaluation.value.ParticularValueFactory;
import proguard.evaluation.value.RangeValueFactory;
import proguard.evaluation.value.TypedReferenceValueFactory;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * This sample application illustrates how to evaluate the bytecode of a method
 * to get information about its control flow and data flow. In this example,
 * it prints out the results of the analysis for each method in the input.
 * For each instruction, it prints out the possible (abstract, symbolic)
 * values on the stack and in the local variables, after execution.
 *
 * You can select the precision of the analysis.
 *
 * Usage:
 *     java proguard.examples.EvaluateCode [precision] input.jar [classnamefilter [methodnamefilter]]
 *
 * where the precision option can be one of:
 *     -basic         for basic control flow analysis and data flow analysis.
 *     -particular    for more precise numerical evaluation.
 *     -range         for evaluation with numeric ranges.
 *     -identity      for symbolic numerical evaluation.
 *     -tracing       for evaluation with reference types, tracing their origins.
 *     -typed         for evaluation with more precise reference types.
 *     -array         for evaluation with primitive arrays.
 *     -detailedarray for evaluation with more precise primitive arrays.
 *
 * The optional class name filter and method name filter can have wildcards (*, **, and ?).
 */
public class EvaluateCode
{
    private static final String BASIC         = "-basic";
    private static final String PARTICULAR    = "-particular";
    private static final String RANGE         = "-range";
    private static final String IDENTITY      = "-identity";
    private static final String TRACING       = "-tracing";
    private static final String TYPED         = "-typed";
    private static final String ARRAY         = "-array";
    private static final String DETAILEDARRAY = "-detailedarray";


    public static void main(String[] args)
    {
        // Parse the arguments.
        int argIndex = 0;
        String precision        = args[argIndex].startsWith("-") ? args[argIndex++] : "-basic";
        String inputJarFileName = args[argIndex++];
        String classNameFilter  = argIndex < args.length ? ClassUtil.internalClassName(args[argIndex++]) : "**";
        String methodNameFilter = argIndex < args.length ? args[argIndex++] : "*";

        try
        {
            // Read the program classes and library classes.
            // The latter are necessary to reconstruct the class hierarchy,
            // which is necessary to properly evaluate the code.
            // We're only reading the base jmod here. General code may need
            // additional jmod files.
            String runtimeFileName = System.getProperty("java.home") + "/jmods/java.base.jmod";

            ClassPool libraryClassPool = JarUtil.readJar(runtimeFileName, true);
            ClassPool programClassPool = JarUtil.readJar(inputJarFileName, classNameFilter, false);

            // We may get some warnings about missing dependencies.
            // They're a pain, but for proper results, we really need to have
            // all dependencies.
            PrintWriter printWriter       = new PrintWriter(System.err);
            WarningPrinter warningPrinter = new WarningPrinter(printWriter);

            // Initialize all cross-references.
            InitializationUtil.initialize(programClassPool, libraryClassPool, warningPrinter);

            // Flush the warnings.
            printWriter.flush();

            // Create a partial evaluator for the specified precision.
            PartialEvaluator partialEvaluator =
                createPartialEvaluator(precision);

            // Analyze the specified methods.
            programClassPool.classesAccept(
                new ClassNameFilter(classNameFilter,
                new AllMethodVisitor(
                new MemberNameFilter(methodNameFilter,
                new AllAttributeVisitor(
                new MyEvaluationResultPrinter(partialEvaluator))))));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Creates a partial evaluator for the given precision.
     */
    private static PartialEvaluator createPartialEvaluator(String precision)
    {
        // The partial evaluator and its support classes determine
        // the precision of the analysis. You would typically pick one
        // depending on your application.

        // In this example, the value factory determines the precision.
        ValueFactory valueFactory =
            precision.equals(BASIC)         ? new BasicValueFactory() :
            precision.equals(PARTICULAR)    ? new ParticularValueFactory(new BasicValueFactory(), new ParticularReferenceValueFactory()) :
            precision.equals(RANGE)         ? new RangeValueFactory(new ArrayReferenceValueFactory(), new BasicValueFactory()) :
            precision.equals(IDENTITY)      ? new IdentifiedValueFactory() :
            precision.equals(TRACING)       ? new ReferenceTracingValueFactory(new BasicValueFactory()) :
            precision.equals(TYPED)         ? new TypedReferenceValueFactory() :
            precision.equals(ARRAY)         ? new ArrayReferenceValueFactory() :
            precision.equals(DETAILEDARRAY) ? new DetailedArrayValueFactory() :
                                              unknownPrecision(precision);

        // In this example, we pick an invocation unit that doesn't try to
        // propagate values across fields and methods.
        InvocationUnit invocationUnit =
            precision.equals(TRACING)       ? new ReferenceTracingInvocationUnit(new BasicInvocationUnit(valueFactory)) :
                                              new BasicInvocationUnit(valueFactory);

        // Create a partial evaluator with this value factory and invocation
        // unit. Don't try to evaluate unreachable code.
        return
            precision.equals(TRACING)       ? new PartialEvaluator(valueFactory, invocationUnit, false, (InstructionVisitor)valueFactory) :
                                              new PartialEvaluator(valueFactory, invocationUnit, false);
    }


    private static ValueFactory unknownPrecision(String precision)
    {
        throw new IllegalArgumentException("Unknown precision ["+precision+"]");
    }


    /**
     * This AttributeVisitor performs symbolic evaluation of the code of
     * each code attribute that it visits and then prints out information
     * about the its stack and local variables after each instruction.
     */
    private static class MyEvaluationResultPrinter
    implements           AttributeVisitor,
                         InstructionVisitor
    {
        private final PartialEvaluator partialEvaluator;


        /**
         * Creates a new analyzer.
         * @param partialEvaluator the partial evaluator that determines the
         *                         precision of the analysis.
         */
        public MyEvaluationResultPrinter(PartialEvaluator partialEvaluator)
        {
            this.partialEvaluator = partialEvaluator;
        }

        // Implementations for AttributeVisitor.

        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


        public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
        {
            // Evaluate the code.
            partialEvaluator.visitCodeAttribute(clazz, method, codeAttribute);

            // Print out a table header for the instructions.
            System.out.print("Instruction | Stack");
            for (int index = 0; index < codeAttribute.u2maxLocals; index++)
            {
                System.out.print(" | v"+index);
            }
            System.out.println(" |");

            System.out.print("------------|-------|");
            for (int index = 0; index < codeAttribute.u2maxLocals; index++)
            {
                System.out.print("----|");
            }
            System.out.println();

            // Go over all instructions to print out some information about
            // the results.
            codeAttribute.instructionsAccept(clazz, method, this);
        }


        // Implementations for InstructionVisitor.

        public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
        {
            // Was the instruction reachable?
            if (partialEvaluator.isTraced(offset))
            {
                // Print out the instruction.
                System.out.print(instruction.toString(offset)+" | ");

                // Print out the stack.
                TracedStack stack = partialEvaluator.getStackAfter(offset);
                for (int index = 0; index < stack.size(); index++)
                {
                    Value actualProducerValue = stack.getBottomActualProducerValue(index);
                    Value producerValue       = stack.getBottomProducerValue(index);
                    Value value               = stack.getBottom(index);
                    System.out.print("[" + string(actualProducerValue, producerValue, value) + "] ");
                }

                // Print out the local variables.
                TracedVariables variables = partialEvaluator.getVariablesAfter(offset);
                for (int index = 0; index < variables.size(); index++)
                {
                    Value producerValue = variables.getProducerValue(index);
                    Value value         = variables.getValue(index);
                    System.out.print(" | " + string(null, producerValue, value));
                }

                System.out.println(" |");
            }
        }


        /**
         * Creates a readable representation of the given value and its
         * origins.
         *
         * @param actualProducerValue the original producers of the value:
         *                            parameters, fields, methods, "new"
         *                            instructions, etc (as
         *                            InstructionOffsetValue).
         * @param producerValue       the instructions that put the value in
         *                            its location on the stack or in the local
         *                            variables (as InstructionOffsetValue).
         * @param value               the value itself.
         */
        private String string(Value actualProducerValue,
                              Value producerValue,
                              Value value)
        {
            StringBuilder builder = new StringBuilder();

            if (actualProducerValue != null)
            {
                builder.append(actualProducerValue);
            }

            if (producerValue != null &&
                !producerValue.equals(actualProducerValue))
            {
                builder.append(producerValue);
            }

            builder.append(value != null ? value : "empty");

            return builder.toString();
        }
    }
}
