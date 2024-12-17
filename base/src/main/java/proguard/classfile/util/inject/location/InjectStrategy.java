package proguard.classfile.util.inject.location;

import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;

/**
 * This interface defines methods for determining code attribute offsets suitable for injecting a
 * method invocation.
 *
 * @author Kymeng Tang
 */
public interface InjectStrategy {
  /**
   * Determine one location (i.e., offset) suitable for injecting a method invocation.
   *
   * @param targetClass The class holding the method in which a method invocation shall be injected
   *     into.
   * @param targetMethod the target method to have a method invocation injected into.
   * @return An InjectLocation instance indicating an offset suitable for injection and whether the
   *     injection shall happen before or after the offset.
   */
  InjectLocation getSingleInjectionLocation(ProgramClass targetClass, ProgramMethod targetMethod);
  /**
   * Determine all locations (i.e., offsets) suitable for injecting a method invocation.
   *
   * @param targetClass The class holding the method in which a method invocation shall be injected
   *     into.
   * @param targetMethod the target method to have a method invocation injected into.
   * @return An array of InjectLocation instances indicating offsets suitable for injection and
   *     whether the injection shall take place before or after each offset.
   */
  InjectLocation[] getAllSuitableInjectionLocation(
      ProgramClass targetClass, ProgramMethod targetMethod);

  /**
   * A data structure indicating a suitable location for injecting a method invocation instruction.
   */
  class InjectLocation {
    // Instruction offset in the code attribute.
    private final int offset;
    // Indicate whether an invocation instruction shall be injected before an offset specified in
    // the offset field.
    private final boolean shouldInjectBefore;

    public InjectLocation(int offset, boolean shouldInjectBefore) {
      this.offset = offset;
      this.shouldInjectBefore = shouldInjectBefore;
    }

    public int getOffset() {
      return offset;
    }

    public boolean shouldInjectBefore() {
      return shouldInjectBefore;
    }
  }
}
