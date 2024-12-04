package proguard.classfile;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/** A minimal implementation of {@link MethodInfo}. */
public class BasicMethodInfo implements MethodInfo {
  private final String methodName;
  private final MethodDescriptor descriptor;

  /**
   * Creates a {@link BasicMethodInfo} given a method name and descriptor.
   *
   * @param descriptor a method descriptor
   * @param methodName a method name
   */
  public BasicMethodInfo(@NotNull String methodName, @NotNull MethodDescriptor descriptor) {
    Objects.requireNonNull(methodName);
    Objects.requireNonNull(descriptor);
    this.methodName = methodName;
    this.descriptor = descriptor;
  }

  /**
   * Creates a {@link BasicMethodInfo} given a {@link MethodInfo}.
   *
   * @param method a method info
   */
  public BasicMethodInfo(MethodInfo method) {
    this(method.getMethodName(), method.getDescriptor());
  }

  @Override
  public @NotNull String getMethodName() {
    return methodName;
  }

  @Override
  public @NotNull MethodDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BasicMethodInfo)) return false;
    BasicMethodInfo that = (BasicMethodInfo) o;
    return Objects.equals(methodName, that.getMethodName())
        && Objects.equals(descriptor, that.getDescriptor());
  }

  @Override
  public int hashCode() {
    return Objects.hash(methodName, descriptor);
  }

  @Override
  public String toString() {
    return methodName + descriptor;
  }
}
