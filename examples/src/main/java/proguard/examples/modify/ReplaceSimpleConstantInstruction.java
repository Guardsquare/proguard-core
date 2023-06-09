package proguard.examples.modify;

import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.ConstantPoolShrinker;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.editor.InstructionSequenceReplacer;
import proguard.classfile.editor.PeepholeEditor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.visitor.MemberNameFilter;
import proguard.examples.CreateHelloWorldClass;

import java.lang.reflect.InvocationTargetException;

import static proguard.examples.util.ExampleUtil.executeMainMethod;

/**
 * Example showing how to use {@link InstructionSequenceReplacer}
* to replace an `LDC` loading "Hello World" to load the constant "Hallo Wereld" instead.
 */
public class ReplaceSimpleConstantInstruction
{

    private static final String NAME_JAVA_IO_PRINTSTREAM = "java/io/PrintStream";
    private static final String METHOD_NAME_PRINTLN = "println";
    private static final String METHOD_TYPE_PRINTLN = "(Ljava/lang/String;)V";

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // Create a sample class by re-using the CreateHelloWorldClass example
        ProgramClass helloWorld = CreateHelloWorldClass.createMessagePrintingClass("HelloWorld", "Hello World");

        // Create an InstructionSequenceBuilder: this is used
        // to build sequences of instructions for both the matcher sequence
        // and the replacement sequence.
        InstructionSequenceBuilder builder = new InstructionSequenceBuilder();

        // The sequence of instructions to match.
        Instruction[] match = builder
                .ldc("Hello World")
                .invokevirtual(NAME_JAVA_IO_PRINTSTREAM, METHOD_NAME_PRINTLN, METHOD_TYPE_PRINTLN)
                .__();

        // The sequence of instructions to replace when there is a match.
        Instruction[] replace = builder
                .ldc("Hallo Wereld")
                .invokevirtual(NAME_JAVA_IO_PRINTSTREAM, METHOD_NAME_PRINTLN, METHOD_TYPE_PRINTLN)
                .__();

        // Constants created by the InstructionSequenceBuilder
        // for example, "Hallo Wereld" is a new constant that needs to
        // be added to the class when instructions are replaced.
        Constant[] constants = builder.constants();

        // A CodeAttributeEditor is the main tool for editing
        // the existing code in a method.
        CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();

        // The PeepholeEditor uses the CodeAttributeEditor and an
        // InstructionSequenceReplacer to modify match instructions
        // and modify the method with replacement instructions.
        PeepholeEditor dutchTranslator = new PeepholeEditor(
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

        // Apply the dutchTranslator to the main method's code attribute.
        helloWorld.methodsAccept(
                new MemberNameFilter("main",
                new AllAttributeVisitor(dutchTranslator)));

        // Run the ConstantPoolShrinker to remove the now unused
        // "Hello World" string from the constant pool.
        helloWorld.accept(new ConstantPoolShrinker());

        // Load the class and execute the "main" method.
        // If all went well, the output should be "Hallo Wereld"!
        executeMainMethod(helloWorld);
    }
}
