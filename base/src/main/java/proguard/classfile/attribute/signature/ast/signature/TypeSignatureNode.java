package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.descriptor.BaseTypeNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class TypeSignatureNode {
  private @Nullable ReferenceTypeSignatureNode referenceTypeSignature;
  private @Nullable BaseTypeNode baseType;

  public TypeSignatureNode(@NotNull BaseTypeNode baseType) {
    if (baseType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.referenceTypeSignature = null;
    this.baseType = baseType;
  }

  public TypeSignatureNode(@NotNull ReferenceTypeSignatureNode referenceTypeSignature) {
    if (referenceTypeSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.referenceTypeSignature = referenceTypeSignature;
    this.baseType = null;
  }

  public boolean isReferenceType() {
    return this.referenceTypeSignature != null;
  }

  public @Nullable ReferenceTypeSignatureNode getReferenceTypeSignature() {
    return referenceTypeSignature;
  }

  public void changeToReferenceType(@NotNull ReferenceTypeSignatureNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.referenceTypeSignature = node;
    this.baseType = null;
  }

  public boolean isBaseType() {
    return this.baseType != null;
  }

  public @Nullable BaseTypeNode getBaseType() {
    return baseType;
  }

  public void changeToBaseType(@NotNull BaseTypeNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.baseType = node;
    this.referenceTypeSignature = null;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    if (referenceTypeSignature != null) {
      return referenceTypeSignature.toString();
    } else if (baseType != null) {
      return baseType.toString();
    } else {
      throw new ASTStructureException("At least one of the fields must be non-null.");
    }
  }
}
