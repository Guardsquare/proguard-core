package proguard.classfile.util;

import java.util.ArrayList;
import java.util.List;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.AllInstructionVisitor;
import proguard.classfile.instruction.visitor.InstructionOpCodeFilter;
import proguard.classfile.instruction.visitor.InstructionVisitor;

/**
 * This utility class finds the offset of a provided list of opcodes in the code attributes.
 *
 * @author Kymeng Tang
 */
public class OpcodeOffsetFinder implements AttributeVisitor, InstructionVisitor, ConstantVisitor {
  private final int[] targetOpcodes;
  private final List<Integer> foundOffsets = new ArrayList<>();

  public OpcodeOffsetFinder(int[] targetOpcodes) {
    this.targetOpcodes = targetOpcodes;
  }

  public List<Integer> getFoundOffsets() {
    return foundOffsets;
  }

  public void reset() {
    foundOffsets.clear();
  }

  // Implementations for AttributeVisitor.

  @Override
  public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

  @Override
  public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
    codeAttribute.accept(
        clazz, method, new AllInstructionVisitor(new InstructionOpCodeFilter(targetOpcodes, this)));
  }

  // Implementations for InstructionVisitor.

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
