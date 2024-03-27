package proguard.classfile;

import org.jetbrains.annotations.NotNull;

/** Can be implemented by classes carrying method information. */
public interface MethodInfo {

  /** Returns the method's name. */
  @NotNull
  String getMethodName();

  /** Returns the method's descriptor. */
  @NotNull
  MethodDescriptor getDescriptor();
}
