package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class FieldTypeNode {
  private @Nullable BaseTypeNode baseType;
  private @Nullable ClassTypeNode classType;
  private @Nullable ArrayTypeNode arrayType;

  public FieldTypeNode(@NotNull BaseTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null");
    }

    this.baseType = type;
    this.classType = null;
    this.arrayType = null;
  }

  public FieldTypeNode(@NotNull ClassTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null");
    }

    this.classType = type;
    this.baseType = null;
    this.arrayType = null;
  }

  public FieldTypeNode(@NotNull ArrayTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null");
    }

    this.arrayType = type;
    this.baseType = null;
    this.classType = null;
  }

  public boolean isBaseType() {
    return this.baseType != null;
  }

  public @Nullable BaseTypeNode getBaseType() {
    return baseType;
  }

  public void changeToBaseType(@NotNull BaseTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null");
    }

    this.baseType = type;
    this.classType = null;
    this.arrayType = null;
  }

  public boolean isClassType() {
    return this.classType != null;
  }

  public @Nullable ClassTypeNode getClassType() {
    return classType;
  }

  public void changeToClassType(@NotNull ClassTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null");
    }

    this.classType = type;
    this.arrayType = null;
    this.baseType = null;
  }

  public boolean isArrayType() {
    return this.arrayType != null;
  }

  public @Nullable ArrayTypeNode getArrayType() {
    return arrayType;
  }

  public void changeToArrayType(@NotNull ArrayTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null");
    }

    this.arrayType = type;
    this.baseType = null;
    this.classType = null;
  }

  @Override
  public String toString() {
    if (this.baseType != null) {
      return baseType.toString();
    } else if (this.classType != null) {
      return classType.toString();
    } else if (this.arrayType != null) {
      return arrayType.toString();
    } else {
      throw new ASTStructureException("At least one of the fields must be non-null");
    }
  }
}
