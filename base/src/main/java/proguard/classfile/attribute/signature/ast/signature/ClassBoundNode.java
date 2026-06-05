package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.visitor.ASTNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ClassBoundNode implements ASTNode {
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

  @Override
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
