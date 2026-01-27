package proguard.classfile.visitor;

import proguard.classfile.AccessConstants;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.TypeConstants;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.editor.AttributesEditor;
import proguard.classfile.editor.ClassEditor;
import proguard.classfile.editor.CodeAttributeComposer;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.classfile.editor.InstructionSequenceBuilder;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.AllParameterVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.MemberFinder;

/**
 * This class visitor is able to create a method and add it to a program class in case it does not
 * exist yet.
 */
public class MethodCreator implements ClassVisitor {
  private static final boolean DEBUG = false;
  private static final MemberFinder strictMemberFinder = new MemberFinder(false);
  private static final MemberFinder memberFinder = new MemberFinder(true);

  private final ClassPool programClassPool;
  private final ClassPool libraryClassPool;
  private final String name;
  private final String descriptor;
  private final int requiredSetAccessFlags;
  private final int processingFlags;

  private boolean callSuper;
  private ProgramMethod method;

  /**
   * Create a new MethodCreator.
   *
   * @param programClassPool The program class pool.
   * @param libraryClassPool The library class pool.
   * @param name The name of the method to add.
   * @param descriptor The descriptor of the method to add.
   * @param requiredSetAccessFlags The access flags of the method to add.
   * @param processingFlags The processing flags to attach to the method.
   * @param callSuper Whether the method should call a super method.
   */
  public MethodCreator(
      ClassPool programClassPool,
      ClassPool libraryClassPool,
      String name,
      String descriptor,
      int requiredSetAccessFlags,
      int processingFlags,
      boolean callSuper) {
    this.programClassPool = programClassPool;
    this.libraryClassPool = libraryClassPool;
    this.name = name;
    this.descriptor = descriptor;
    this.requiredSetAccessFlags = requiredSetAccessFlags;
    this.processingFlags = processingFlags;
    this.callSuper = callSuper;
  }

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {}

  @Override
  public void visitProgramClass(ProgramClass programClass) {
    ClassEditor classEditor = new ClassEditor(programClass);

    // Check if the method exists, and if not create it.
    if (strictMemberFinder.findMethod(programClass, name, descriptor) != null) {
      if (DEBUG) {
        System.out.println(
            "Not creating method " + programClass.getName() + "." + name + descriptor);
      }

      return;
    }

    ConstantPoolEditor constantPoolEditor = new ConstantPoolEditor(programClass);

    ProgramMethod method =
        new ProgramMethod(
            requiredSetAccessFlags,
            constantPoolEditor.addUtf8Constant(name),
            constantPoolEditor.addUtf8Constant(descriptor),
            null);

    if (DEBUG) {
      System.out.println("Creating method " + programClass.getName() + "." + name + descriptor);
    }

    // Find the superclass and method. Note that the hierarchy should be initialized for this.
    Method superMethod = memberFinder.findMethod(programClass, name, descriptor);
    Clazz superClass = memberFinder.correspondingClass();

    // The super-method cannot be final.
    if (superMethod instanceof ProgramMethod
        && (((ProgramMethod) superMethod).u2accessFlags & AccessConstants.FINAL) != 0) {
      ((ProgramMethod) superMethod).u2accessFlags &= ~AccessConstants.FINAL;
      // We should always call super when we are making the super-method non-final, as it was
      // marked to not change through inheritance.
      callSuper = true;
    }

    InstructionSequenceBuilder builder =
        new InstructionSequenceBuilder(programClass, programClassPool, libraryClassPool);

    // Create a super call if we need to.
    if (callSuper && superMethod != null) {
      if ((requiredSetAccessFlags & AccessConstants.STATIC) != 0) {
        throw new RuntimeException(
            "You cannot use 'callSuper' with static methods. "
                + "New method: "
                + programClass.getName()
                + "."
                + name
                + descriptor
                + ". "
                + "Super method: "
                + superClass.getName()
                + "."
                + name
                + descriptor
                + ".");
      }

      if ((superMethod.getAccessFlags() & AccessConstants.ABSTRACT) != 0) {
        throw new RuntimeException(
            "Cannot call super when super method is abstract. "
                + "New method: "
                + programClass.getName()
                + "."
                + name
                + descriptor
                + ". "
                + "Super method: "
                + superClass.getName()
                + "."
                + name
                + descriptor
                + ".");
      }

      String superMethodName = superMethod.getName(superClass);
      String superMethodDescriptor = superMethod.getDescriptor(superClass);

      if (DEBUG) {
        System.out.println(
            "Super method: "
                + superClass.getName()
                + "."
                + superMethodName
                + superMethodDescriptor);
      }

      builder.aload(0);

      superMethod.accept(
          superClass,
          new AllParameterVisitor(
              false,
              (clazz,
                  member,
                  parameterIndex,
                  parameterCount,
                  parameterOffset,
                  parameterSize,
                  parameterType,
                  referencedClass) -> builder.load(parameterOffset + 1, parameterType.charAt(0))));

      builder.invokespecial(
          programClass.getSuperClass().getName(), superMethodName, superMethodDescriptor);

      String returnType = ClassUtil.internalMethodReturnType(superMethodDescriptor);
      addReturnInstructions(builder, returnType);
    } else {
      builder.return_();
    }

    CodeAttribute codeAttribute =
        new CodeAttribute(constantPoolEditor.addUtf8Constant(Attribute.CODE));
    CodeAttributeComposer codeComposer = new CodeAttributeComposer();
    Instruction[] instructions = builder.__();

    // Add the code to the method.
    codeComposer.beginCodeFragment(instructions.length);
    for (int index = 0; index < instructions.length; index++) {
      codeComposer.appendInstruction(index, instructions[index]);
    }
    codeComposer.endCodeFragment();

    codeComposer.visitCodeAttribute(programClass, method, codeAttribute);
    new AttributesEditor(programClass, method, false).addAttribute(codeAttribute);

    // Add the method to the class.
    classEditor.addMethod(method);

    // Attach processing flags.
    method.setProcessingFlags(processingFlags);

    this.method = method;
  }

  /**
   * Adds a return instruction to this builder for a super call that resulted in the given return
   * type.
   */
  public static void addReturnInstructions(InstructionSequenceBuilder builder, String returnType) {
    // Make sure that we can create some very basic methods that return the value from a super call
    // at the end.
    if (ClassUtil.isInternalClassType(returnType) || ClassUtil.isInternalArrayType(returnType)) {
      // Super call returns an object.
      builder.areturn();
    } else if (ClassUtil.isInternalPrimitiveType(returnType)
        || returnType.equals(TypeConstants.VOID + "")) {
      // Super call returns a primitive.
      switch (returnType.charAt(0)) {
        case TypeConstants.BOOLEAN:
        case TypeConstants.BYTE:
        case TypeConstants.CHAR:
        case TypeConstants.SHORT:
        case TypeConstants.INT:
          builder.ireturn();
          break;
        case TypeConstants.LONG:
          builder.lreturn();
          break;
        case TypeConstants.FLOAT:
          builder.freturn();
          break;
        case TypeConstants.DOUBLE:
          builder.dreturn();
          break;
        case TypeConstants.VOID:
          builder.return_();
          break;
      }
    } else {
      // Should never happen.
      throw new RuntimeException("Unexpected type: " + returnType);
    }
  }

  public ProgramMethod getMethod() {
    return method;
  }
}
