package proguard.examples.modify;

import static proguard.examples.util.ExampleUtil.createClassPool;
import static proguard.examples.util.ExampleUtil.executeMainMethod;

import com.example.SampleClassWithObjects;
import proguard.classfile.AccessConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.editor.ClassBuilder;
import proguard.classfile.editor.CodeAttributeEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.editor.PeepholeEditor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.BranchTargetFinder;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.MemberNameFilter;

/**
 * Example showing how to use the {@link PeepholeEditor} with a {@link BranchTargetFinder} and a
 * {@link CodeAttributeEditor} to add code before and after branches.
 */
public class EditClass {
  private static final String NAME_JAVA_IO_PRINTSTREAM = "java/io/PrintStream";
  private static final String NAME_JAVA_LANG_INTEGER = "java/lang/Integer";
  public static final String NAME_JAVA_LANG_SYSTEM = "java/lang/System";
  private static final String METHOD_NAME_PRINTLN = "println";
  private static final String METHOD_NAME_INIT = "<init>";
  public static final String METHOD_NAME_GETANSWER = "getAnswer";
  public static final String FIELD_NAME_OUT = "out";
  private static final String METHOD_TYPE_PRINTLN = "(Ljava/lang/String;)V";
  private static final String METHOD_TYPE_INT_VOID = "(I)V";
  private static final String METHOD_TYPE_OBJECT_V = "(Ljava/lang/Object;)V";
  public static final String METHOD_TYPE_NUMBER_NUMBER = "(Ljava/lang/Number;)Ljava/lang/Number;";
  public static final String TYPE_PRINTSTREAM = "Ljava/io/PrintStream;";

  public static void main(String[] args) {
    // Load the example class into a ClassPool.
    // The example has a method "Number getAnswer(Number)"
    // which returns 42 if the argument is null otherwise it
    // returns the Number that was passed as an argument.
    ClassPool programClassPool = createClassPool(SampleClassWithObjects.class);
    String sampleInternalClassName =
        ClassUtil.internalClassName(SampleClassWithObjects.class.getName());
    ProgramClass sampleClassWithObjects =
        (ProgramClass) programClassPool.getClass(sampleInternalClassName);

    // We can use tha ClassBuilder for an existing class as well.
    ClassBuilder classBuilder = new ClassBuilder(sampleClassWithObjects);

    // Add a main method to our class which will call getAnswer()
    // and print the result.
    classBuilder.addMethod(
        AccessConstants.PUBLIC | AccessConstants.STATIC,
        "main",
        "([Ljava/lang/String;)V",
        50,

        // Compose the equivalent of this java code:
        //     System.out.println(getAnswer((Number)null));
        //     System.out.println(getAnswer(new Integer(42)));
        code ->
            code.getstatic(NAME_JAVA_LANG_SYSTEM, FIELD_NAME_OUT, TYPE_PRINTSTREAM)
                .aconst_null()
                .invokestatic(
                    sampleInternalClassName, METHOD_NAME_GETANSWER, METHOD_TYPE_NUMBER_NUMBER)
                .invokevirtual(NAME_JAVA_IO_PRINTSTREAM, METHOD_NAME_PRINTLN, METHOD_TYPE_OBJECT_V)
                .getstatic(NAME_JAVA_LANG_SYSTEM, FIELD_NAME_OUT, TYPE_PRINTSTREAM)
                .new_(NAME_JAVA_LANG_INTEGER)
                .dup()
                .bipush(42)
                .invokespecial(NAME_JAVA_LANG_INTEGER, METHOD_NAME_INIT, METHOD_TYPE_INT_VOID)
                .invokestatic(
                    sampleInternalClassName, METHOD_NAME_GETANSWER, METHOD_TYPE_NUMBER_NUMBER)
                .invokevirtual(NAME_JAVA_IO_PRINTSTREAM, METHOD_NAME_PRINTLN, METHOD_TYPE_OBJECT_V)
                .return_());

    // A CodeAttributeEditor is the main tool for editing
    // the existing code in a method.
    CodeAttributeEditor codeAttributeEditor = new CodeAttributeEditor();
    // A BranchTargetFinder can be used to find the branches
    // of the given codeAttribute.
    BranchTargetFinder branchTargetFinder = new BranchTargetFinder();
    // We can use a filter in order to only visit the method
    // we are interested in.
    sampleClassWithObjects.methodsAccept(
        new MemberNameFilter(
            METHOD_NAME_GETANSWER,
            new AllAttributeVisitor(
                // The PeepholeEditor uses the CodeAttributeEditor, the
                // BranchTargetFinder and our InstructionVisitor to add
                // instructions and modify the method that we visit.
                new PeepholeEditor(
                    branchTargetFinder,
                    codeAttributeEditor,
                    new MyInstructionVisitor(branchTargetFinder, codeAttributeEditor)))));
    // If all went well, the output should be:
    // Hello from inside a branch statement
    // Hello before a return statement
    // 42
    // Hello before a return statement
    // 42
    executeMainMethod(sampleClassWithObjects);
  }

  private static class MyInstructionVisitor implements InstructionVisitor {
    private final BranchTargetFinder branchTargetFinder;
    private final CodeAttributeEditor codeAttributeEditor;

    public MyInstructionVisitor(
        BranchTargetFinder branchTargetFinder, CodeAttributeEditor codeAttributeEditor) {
      this.branchTargetFinder = branchTargetFinder;
      this.codeAttributeEditor = codeAttributeEditor;
    }

    // Implementations for InstructionVisitor.

    @Override
    public void visitAnyInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        Instruction instruction) {
      if (branchTargetFinder.isBranchOrigin(offset)) {
        InstructionSequenceBuilder builder = new InstructionSequenceBuilder((ProgramClass) clazz);
        // In the case of a return or a goto instruction, we want to
        // add the print statement before the instruction.
        if (isReturnInstruction(instruction.opcode) || isGotoInstruction(instruction.opcode)) {
          Instruction[] addedInstructions =
              builder
                  .getstatic(NAME_JAVA_LANG_SYSTEM, FIELD_NAME_OUT, TYPE_PRINTSTREAM)
                  .ldc("Hello before a return statement")
                  .invokevirtual(NAME_JAVA_IO_PRINTSTREAM, METHOD_NAME_PRINTLN, METHOD_TYPE_PRINTLN)
                  .instructions();
          codeAttributeEditor.insertBeforeInstruction(offset, addedInstructions);
        }
        // In all other cases we want to add the print statement after the instruction.
        else {
          Instruction[] addedInstructions =
              builder
                  .getstatic(NAME_JAVA_LANG_SYSTEM, FIELD_NAME_OUT, TYPE_PRINTSTREAM)
                  .ldc("Hello from inside a branch statement")
                  .invokevirtual(NAME_JAVA_IO_PRINTSTREAM, METHOD_NAME_PRINTLN, METHOD_TYPE_PRINTLN)
                  .instructions();
          codeAttributeEditor.insertAfterInstruction(offset, addedInstructions);
        }
      }
    }

    // Small utility methods.

    private static boolean isReturnInstruction(int opcode) {
      switch (opcode) {
        case Instruction.OP_RET:
        case Instruction.OP_RETURN:
        case Instruction.OP_ARETURN:
        case Instruction.OP_DRETURN:
        case Instruction.OP_IRETURN:
        case Instruction.OP_LRETURN:
        case Instruction.OP_FRETURN:
          return true;
        default:
          return false;
      }
    }

    private static boolean isGotoInstruction(int opcode) {
      switch (opcode) {
        case Instruction.OP_GOTO:
        case Instruction.OP_GOTO_W:
          return true;
        default:
          return false;
      }
    }
  }
}
