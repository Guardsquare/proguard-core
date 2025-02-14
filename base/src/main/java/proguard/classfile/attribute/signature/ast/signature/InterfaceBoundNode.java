package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class InterfaceBoundNode {
  private @NotNull ReferenceTypeSignatureNode interfaceReference;

  public InterfaceBoundNode(@NotNull ReferenceTypeSignatureNode referenceTypeSignature) {
    if (referenceTypeSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.interfaceReference = referenceTypeSignature;
  }

  public @NotNull ReferenceTypeSignatureNode getInterfaceReference() {
    return interfaceReference;
  }

  public void setInterfaceReference(@NotNull ReferenceTypeSignatureNode interfaceReference) {
    if (interfaceReference == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.interfaceReference = interfaceReference;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    return ":" + interfaceReference.toString();
  }
}
