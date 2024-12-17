package proguard.classfile.util;

import proguard.classfile.ClassConstants;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.MethodrefConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.AllInstructionVisitor;
import proguard.classfile.instruction.visitor.InstructionOpCodeFilter;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This utility class finds the offset of the invocation to the current or super class constructor
 * after visiting an <init> method.
 *
 * @author Kymeng Tang
 */
public class ConstructorInvocationOffsetFinder implements MemberVisitor {
  private int initOffset = -1;

  public int getConstructorCallOffset() {
    assert initOffset != -1
        : "The constructor invocation offset is being requested before visiting any <init> member "
            + "after instantiation or resetting.";
    return initOffset;
  }

  public void reset() {
    this.initOffset = -1;
  }

  // MemberVisitor implementation
  @Override
  public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
    assert programMethod.getName(programClass).equals(ClassConstants.METHOD_NAME_INIT)
        : this.getClass().getName()
            + " only supports constructor but "
            + programClass.getName()
            + "."
            + programMethod.getName(programClass)
            + programMethod.getDescriptor(programClass)
            + " is being visited.";

    assert initOffset == -1
        : "This instance of "
            + this.getClass().getName()
            + " has already visited an <init> member; "
            + "To avoid overriding the previously found offset, please store the return value of "
            + "getConstructorCallOffset(), and call reset() method.";

    programMethod.attributesAccept(
        programClass, new AttributeNameFilter(Attribute.CODE, new ConstructorOffsetFinderImpl()));
  }

  private class ConstructorOffsetFinderImpl
      implements AttributeVisitor, InstructionVisitor, ConstantVisitor {
    // AttributeVisitor implementation
    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
      codeAttribute.accept(
          clazz,
          method,
          new AllInstructionVisitor(
              new InstructionOpCodeFilter(new int[] {Instruction.OP_INVOKESPECIAL}, this)));
    }

    // InstructionVisitor implementation
    @Override
    public void visitAnyInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        Instruction instruction) {}

    @Override
    public void visitConstantInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        ConstantInstruction constantInstruction) {
      clazz.constantPoolEntryAccept(
          constantInstruction.constantIndex,
          new ConstantVisitor() {
            @Override
            public void visitAnyConstant(Clazz clazz, Constant constant) {}

            @Override
            public void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant) {
              if (methodrefConstant.getName(clazz).equals(ClassConstants.METHOD_NAME_INIT)) {
                initOffset = offset;
              }
            }
          });
    }
  }
}
