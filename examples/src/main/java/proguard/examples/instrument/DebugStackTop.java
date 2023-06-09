package proguard.examples.instrument;

import proguard.classfile.ClassPool;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.editor.InstructionSequenceReplacer;
import proguard.classfile.editor.PeepholeEditor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.AllMethodVisitor;
import proguard.classfile.visitor.MemberNameFilter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static proguard.examples.util.ExampleUtil.createClassPool;
import static proguard.examples.util.ExampleUtil.executeMainMethod;

/**
 * Example showing how to use an {@link InstructionSequenceReplacer}
 * to add logging before a method call which prints the value on the top of the stack
 * i.e. a method parameter.
 */
public class DebugStackTop
{
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // Load the example class into a ClassPool.
        // The example has a method "foo(String)" which
        // is called with program arguments, args[0] and args[1].
        ClassPool programClassPool = createClassPool(DebugStackTopExample.class);

        // Names in ProGuardCORE are represented as "internal" names
        // that use `/` as the package separator instead of `.`: this
        // is to match the Java Classfile Specification format.
        String exampleInternalClassName = ClassUtil.internalClassName(DebugStackTopExample.class.getName());

        // Create an InstructionSequenceBuilder: this is used
        // to build sequences of instructions for both the matcher sequence
        // and the replacement sequence.
        InstructionSequenceBuilder builder = new InstructionSequenceBuilder();

        // The sequence of instructions to match.
        Instruction[] match = builder
            .invokestatic("proguard/examples/instrument/DebugStackTopExample", "foo", "(Ljava/lang/String;)V")
            .__();

        // The sequence of instructions to replace when there is a match.
        Instruction[] replace = builder
            .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
            .ldc("Foo called with parameter: ")
            .invokevirtual("java/io/PrintStream", "print", "(Ljava/lang/String;)V")

            // Duplicate the top of the stack.
            .dup()
            // Load System.out onto the stack.
            .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")

            // At this point the stack looks like: [string][string][out]
            // so we need to swap the top two elements to [string][out][string].
            .swap()
            .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")

            // Execute the original method call.
            .invokestatic("proguard/examples/instrument/DebugStackTopExample", "foo", "(Ljava/lang/String;)V")
            .__();

        Constant[] constants = builder.constants();

        // A CodeAttributeEditor is the main tool for editing
        // the existing code in a method.
        CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();

        // The PeepholeEditor uses the CodeAttributeEditor and an
        // InstructionSequenceReplacer to match instructions
        // and modify the method with replacement instructions.
        PeepholeEditor debugInstructionAdder = new PeepholeEditor(
            codeAttributeEditor,
            new InstructionSequenceReplacer(
                constants,
                match,
                constants,
                replace,
                null, // A BranchTargetFinder is not needed in this example.
                codeAttributeEditor
            )
        );

        // Apply the debugInstrumentor to the "main" method.
        programClassPool.classAccept(exampleInternalClassName,
                new AllMethodVisitor(
                new MemberNameFilter("main",
                new AllAttributeVisitor(debugInstructionAdder))));

        // If all went well, "Foo called with parameter: String 1" and
        // "Foo called with parameter: String 2" should be printed!
        executeMainMethod(programClassPool, exampleInternalClassName, "String 1", "String 2");
    }
}
