package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class BoundedTypeArgumentNode {
  private @Nullable WildcardIndicatorNode wildcardIndicator;
  private @NotNull ReferenceTypeSignatureNode referenceTypeSignature;

  public BoundedTypeArgumentNode(
      @Nullable WildcardIndicatorNode wildcardIndicator,
      @NotNull ReferenceTypeSignatureNode referenceTypeSignature) {
    if (referenceTypeSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.wildcardIndicator = wildcardIndicator;
    this.referenceTypeSignature = referenceTypeSignature;
  }

  public @Nullable WildcardIndicatorNode getWildcardIndicator() {
    return wildcardIndicator;
  }

  public void setWildcardIndicator(@Nullable WildcardIndicatorNode wildcardIndicator) {
    this.wildcardIndicator = wildcardIndicator;
  }

  public @NotNull ReferenceTypeSignatureNode getReferenceTypeSignature() {
    return referenceTypeSignature;
  }

  public void setReferenceTypeSignature(
      @NotNull ReferenceTypeSignatureNode referenceTypeSignature) {
    this.referenceTypeSignature = referenceTypeSignature;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    if (wildcardIndicator == null) {
      return referenceTypeSignature.toString();
    } else {
      return wildcardIndicator.toString() + referenceTypeSignature.toString();
    }
  }
}
