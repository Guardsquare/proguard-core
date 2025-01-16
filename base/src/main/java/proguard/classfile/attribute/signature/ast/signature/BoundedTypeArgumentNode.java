package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

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

  @Override
  public String toString() {
    if (wildcardIndicator == null) {
      return referenceTypeSignature.toString();
    } else {
      return wildcardIndicator.toString() + referenceTypeSignature.toString();
    }
  }
}
