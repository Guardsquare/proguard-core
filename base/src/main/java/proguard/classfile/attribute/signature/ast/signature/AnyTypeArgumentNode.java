package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public enum AnyTypeArgumentNode {
  INSTANCE;

  @Override
  public @NotNull String toString() {
    return "*";
  }
}
