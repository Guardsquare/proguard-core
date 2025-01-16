package proguard.classfile.attribute.signature.ast.signature;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ClassSignatureNode {
  private @NotNull List<TypeParameterNode> typeParameters;
  private @NotNull SuperclassSignatureNode superclassSignature;
  private @NotNull List<SuperinterfaceSignatureNode> superinterfaceSignatures;

  public ClassSignatureNode(
      @NotNull List<TypeParameterNode> typeParameters,
      @NotNull SuperclassSignatureNode superclassSignature,
      @NotNull List<SuperinterfaceSignatureNode> superinterfaceSignatures) {
    this.typeParameters = typeParameters;
    this.superclassSignature = superclassSignature;
    this.superinterfaceSignatures = superinterfaceSignatures;
  }

  public @NotNull List<TypeParameterNode> getTypeParameters() {
    return typeParameters;
  }

  public void setTypeParameters(@NotNull List<TypeParameterNode> typeParameters) {
    if (typeParameters == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.typeParameters = typeParameters;
  }

  public @NotNull SuperclassSignatureNode getSuperclassSignature() {
    return superclassSignature;
  }

  public void setSuperclassSignature(@NotNull SuperclassSignatureNode superclassSignature) {
    if (superclassSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.superclassSignature = superclassSignature;
  }

  public @NotNull List<SuperinterfaceSignatureNode> getSuperinterfaceSignatures() {
    return superinterfaceSignatures;
  }

  public void setSuperinterfaceSignatures(
      @NotNull List<SuperinterfaceSignatureNode> superinterfaceSignatures) {
    if (superinterfaceSignatures == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.superinterfaceSignatures = superinterfaceSignatures;
  }

  @Override
  public @NotNull String toString() {
    @NotNull StringBuilder sb = new StringBuilder();
    if (!typeParameters.isEmpty()) {
      sb.append('<');
      for (@NotNull TypeParameterNode typeParameterNode : this.typeParameters) {
        sb.append(typeParameterNode.toString());
      }
      sb.append('>');
    }
    sb.append(superclassSignature.toString());
    for (@NotNull
    SuperinterfaceSignatureNode superinterfaceSignatureNode : superinterfaceSignatures) {
      sb.append(superinterfaceSignatureNode.toString());
    }

    return sb.toString();
  }
}
