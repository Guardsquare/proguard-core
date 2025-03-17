package proguard.classfile.util.inject.location;

import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;

public class SpecificOffset implements InjectStrategy {
  private final int targetOffset;
  private final boolean shouldInjectBefore;

  public SpecificOffset(int targetOffset, boolean shouldInjectBefore) {
    this.targetOffset = targetOffset;
    this.shouldInjectBefore = shouldInjectBefore;
  }

  @Override
  public InjectLocation getSingleInjectionLocation(
      ProgramClass targetClass, ProgramMethod targetMethod) {
    return new InjectLocation(targetOffset, shouldInjectBefore);
  }

  @Override
  public InjectLocation[] getAllSuitableInjectionLocation(
      ProgramClass targetClass, ProgramMethod targetMethod) {
    return new InjectLocation[] {getSingleInjectionLocation(targetClass, targetMethod)};
  }
}
