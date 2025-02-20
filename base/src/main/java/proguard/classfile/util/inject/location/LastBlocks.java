package proguard.classfile.util.inject.location;

import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.OpcodeOffsetFinder;

/**
 * An implementation of the InjectStrategy interface to find the potential last blocks of a method.
 * The last block of a method is defined as the offset of return and throw opcode within a method.
 */
public class LastBlocks implements InjectStrategy {
  private final OpcodeOffsetFinder offsetFinder =
      new OpcodeOffsetFinder(
          new int[] {
            Instruction.OP_RETURN,
            Instruction.OP_ARETURN,
            Instruction.OP_IRETURN,
            Instruction.OP_LRETURN,
            Instruction.OP_FRETURN,
            Instruction.OP_DRETURN,
            Instruction.OP_ATHROW
          });

  /**
   * Find the first offset of return or throw instructions to inject a method invocation.
   *
   * @param targetClass The class holding the method in which a method invocation shall be injected
   *     into.
   * @param targetMethod the target method to have a method invocation injected into.
   * @return An InjectLocation instance indicating the first offset suitable for injection.
   */
  @Override
  public InjectLocation getSingleInjectionLocation(
      ProgramClass targetClass, ProgramMethod targetMethod) {
    InjectLocation[] foundOffsets = this.getAllSuitableInjectionLocation(targetClass, targetMethod);

    assert foundOffsets.length > 0
        : "No return nor throw opcodes found; are you visiting an abstract method?";

    return foundOffsets[0];
  }

  /**
   * Find offsets of return or throw instructions to inject a method invocation.
   *
   * @param targetClass The class holding the method in which a method invocation shall be injected
   *     into.
   * @param targetMethod the target method to have a method invocation injected into.
   * @return An array of one InjectLocation instance indicating the first offset suitable for
   *     injection.
   */
  @Override
  public InjectLocation[] getAllSuitableInjectionLocation(
      ProgramClass targetClass, ProgramMethod targetMethod) {
    targetMethod.accept(targetClass, offsetFinder);

    return offsetFinder.getFoundOffsets().stream()
        .map(offset -> new InjectLocation(offset, true))
        .toArray(InjectLocation[]::new);
  }
}
