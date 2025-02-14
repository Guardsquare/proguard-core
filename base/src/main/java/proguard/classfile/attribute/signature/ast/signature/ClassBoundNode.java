package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ClassBoundNode {
  private @Nullable ReferenceTypeSignatureNode referenceTypeSignature;

  public ClassBoundNode(@Nullable ReferenceTypeSignatureNode referenceTypeSignature) {
    this.referenceTypeSignature = referenceTypeSignature;
  }

  public @Nullable ReferenceTypeSignatureNode getReferenceTypeSignature() {
    return referenceTypeSignature;
  }

  public void setReferenceTypeSignature(
      @Nullable ReferenceTypeSignatureNode referenceTypeSignature) {
    this.referenceTypeSignature = referenceTypeSignature;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    if (referenceTypeSignature == null) {
      return ":";
    } else {
      return ":" + referenceTypeSignature;
    }
  }
}
