package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ReferenceTypeSignatureNode {
  private @Nullable ClassTypeSignatureNode classType;
  private @Nullable TypeVariableSignatureNode typeVariable;
  private @Nullable ArrayTypeSignatureNode arrayType;

  public ReferenceTypeSignatureNode(@NotNull ClassTypeSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = signature;
    this.arrayType = null;
    this.typeVariable = null;
  }

  public ReferenceTypeSignatureNode(@NotNull TypeVariableSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = null;
    this.arrayType = null;
    this.typeVariable = signature;
  }

  public ReferenceTypeSignatureNode(@NotNull ArrayTypeSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = null;
    this.arrayType = signature;
    this.typeVariable = null;
  }

  public boolean isClassType() {
    return classType != null;
  }

  public @Nullable ClassTypeSignatureNode getClassType() {
    return classType;
  }

  public void changeToClassType(@NotNull ClassTypeSignatureNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = node;
    this.arrayType = null;
    this.typeVariable = null;
  }

  public boolean isTypeVariable() {
    return typeVariable != null;
  }

  public @Nullable TypeVariableSignatureNode getTypeVariable() {
    return typeVariable;
  }

  public void changeToTypeVariable(@NotNull TypeVariableSignatureNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.typeVariable = node;
    this.arrayType = null;
    this.classType = null;
  }

  public boolean isArrayType() {
    return arrayType != null;
  }

  public @Nullable ArrayTypeSignatureNode getArrayType() {
    return arrayType;
  }

  public void changeToArrayType(@NotNull ArrayTypeSignatureNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.arrayType = node;
    this.typeVariable = null;
    this.classType = null;
  }

  @Override
  public String toString() {
    if (classType != null) {
      return classType.toString();
    } else if (typeVariable != null) {
      return typeVariable.toString();
    } else if (arrayType != null) {
      return arrayType.toString();
    } else {
      throw new ASTStructureException("At least one of the fields must be non-null");
    }
  }
}
