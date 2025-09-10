package proguard.classfile.util;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;

/**
 * This {@link InstructionVisitor} will delegate visits only if the visited instructions match the
 * required sequence.
 *
 * @see InstructionSequenceMatcher
 */
public class InstructionSequenceFilter implements InstructionVisitor {

  private final InstructionSequenceMatcher matcher;
  private final InstructionVisitor accepted;

  public InstructionSequenceFilter(
      InstructionSequenceMatcher matcher, InstructionVisitor accepted) {
    this.matcher = matcher;
    this.accepted = accepted;
  }

  // Implementation for InstructionVisitor.

  @Override
  public void visitAnyInstruction(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      Instruction instruction) {
    instruction.accept(clazz, method, codeAttribute, offset, matcher);

    if (matcher.isMatching()) {
      int startOffset = matcher.matchedInstructionOffset(0);
      codeAttribute.instructionsAccept(
          clazz, method, startOffset, offset + instruction.length(offset), accepted);
    }
  }
}
