package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class TypeArgumentNode {
  private @Nullable AnyTypeArgumentNode anyTypeArg;
  private @Nullable BoundedTypeArgumentNode boundedTypeArg;

  public TypeArgumentNode(@NotNull AnyTypeArgumentNode anyTypeArg) {
    if (anyTypeArg == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.anyTypeArg = anyTypeArg;
    this.boundedTypeArg = null;
  }

  public TypeArgumentNode(@NotNull BoundedTypeArgumentNode boundedTypeArg) {
    if (boundedTypeArg == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.boundedTypeArg = boundedTypeArg;
    this.anyTypeArg = null;
  }

  public void changeToAnyTypeArgument() {
    this.anyTypeArg = AnyTypeArgumentNode.INSTANCE;
    this.boundedTypeArg = null;
  }

  public boolean isBounded() {
    return boundedTypeArg != null;
  }

  public @Nullable BoundedTypeArgumentNode getBoundedTypeArg() {
    return boundedTypeArg;
  }

  public void changeToBoundedTypeArgument(@NotNull BoundedTypeArgumentNode boundedTypeArg) {
    if (boundedTypeArg == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.boundedTypeArg = boundedTypeArg;
    this.anyTypeArg = null;
  }

  @Override
  public String toString() {
    if (anyTypeArg != null) {
      return anyTypeArg.toString();
    } else if (boundedTypeArg != null) {
      return boundedTypeArg.toString();
    } else {
      throw new ASTStructureException("At least one of the fields must be non-null");
    }
  }
}
