package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @Override
  public @NotNull String toString() {
    if (referenceTypeSignature == null) {
      return ":";
    } else {
      return ":" + referenceTypeSignature;
    }
  }
}
