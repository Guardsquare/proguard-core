package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

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

  @Override
  public @NotNull String toString() {
    return ":" + interfaceReference.toString();
  }
}
