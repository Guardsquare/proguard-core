package proguard.classfile.util;

import java.util.ArrayList;
import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.AllInstructionVisitor;
import proguard.classfile.instruction.visitor.InstructionOpCodeFilter;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This utility class finds the offset of a provided list of opcode in the code attributes.
 *
 * @author Kymeng Tang
 */
public class OpcodeOffsetFinder implements MemberVisitor {
  private final List<Integer> foundOffsets = new ArrayList<>();
  private final int[] targetOpcodes;

  public OpcodeOffsetFinder(int[] targetOpcodes) {
    this.targetOpcodes = targetOpcodes;
  }

  public List<Integer> getFoundOffsets() {
    return foundOffsets;
  }

  public void reset() {
    this.foundOffsets.clear();
  }

  // MemberVisitor implementation
  @Override
  public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
    assert foundOffsets.size() == 0
        : "This instance of "
            + this.getClass().getName()
            + " has already visited a method; "
            + "To avoid overriding the previously found offset, please store the return value of "
            + "getFoundOffsets(), and call reset() method.";

    programMethod.attributesAccept(
        programClass, new AttributeNameFilter(Attribute.CODE, new OpcodeOffsetFinderImpl()));
  }

  private class OpcodeOffsetFinderImpl
      implements AttributeVisitor, InstructionVisitor, ConstantVisitor {
    // AttributeVisitor implementation
    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
      codeAttribute.accept(
          clazz,
          method,
          new AllInstructionVisitor(new InstructionOpCodeFilter(targetOpcodes, this)));
    }

    // InstructionVisitor implementation
    @Override
    public void visitAnyInstruction(
        Clazz clazz,
        Method method,
        CodeAttribute codeAttribute,
        int offset,
        Instruction instruction) {
      foundOffsets.add(offset);
    }
  }
}
