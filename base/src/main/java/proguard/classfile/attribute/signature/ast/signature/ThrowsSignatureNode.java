package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ThrowsSignatureNode {
  private @Nullable ClassTypeSignatureNode classType;
  private @Nullable TypeVariableSignatureNode typeVariable;

  public ThrowsSignatureNode(@NotNull ClassTypeSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = signature;
    this.typeVariable = null;
  }

  public ThrowsSignatureNode(@NotNull TypeVariableSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = null;
    this.typeVariable = signature;
  }

  public @Nullable ClassTypeSignatureNode getClassType() {
    return classType;
  }

  public void changeToClassType(@NotNull ClassTypeSignatureNode classTypeSignature) {
    if (classTypeSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = classTypeSignature;
    this.typeVariable = null;
  }

  public @Nullable TypeVariableSignatureNode getTypeVariable() {
    return typeVariable;
  }

  public void changeToTypeVariable(@NotNull TypeVariableSignatureNode typeVariableSignature) {
    if (typeVariableSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.typeVariable = typeVariableSignature;
    this.classType = null;
  }

  @Override
  public @NotNull String toString() {
    if (this.typeVariable != null) {
      return typeVariable.toString();
    } else if (this.classType != null) {
      return this.classType.toString();
    } else {
      throw new ASTStructureException("at least one of the fields must be non-null");
    }
  }
}
