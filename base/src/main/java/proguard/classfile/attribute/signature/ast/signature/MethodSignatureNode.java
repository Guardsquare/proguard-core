package proguard.classfile.attribute.signature.ast.signature;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class MethodSignatureNode {
  private @NotNull List<TypeParameterNode> typeParameters;
  private @NotNull List<TypeSignatureNode> argumentTypes;
  private @NotNull ResultNode result;
  private @NotNull List<ThrowsSignatureNode> throwsSignatures;

  public MethodSignatureNode(
      @NotNull List<TypeParameterNode> typeParameters,
      @NotNull List<TypeSignatureNode> argumentTypes,
      @NotNull ResultNode result,
      @NotNull List<ThrowsSignatureNode> throwsSignatures) {
    if (typeParameters == null
        || argumentTypes == null
        || result == null
        || throwsSignatures == null) {
      throw new ASTStructureException("Arguments must no be null");
    }
    this.typeParameters = typeParameters;
    this.argumentTypes = argumentTypes;
    this.result = result;
    this.throwsSignatures = throwsSignatures;
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

  public @NotNull List<TypeSignatureNode> getArgumentTypes() {
    return argumentTypes;
  }

  public void setArgumentTypes(@NotNull List<TypeSignatureNode> argumentTypes) {
    if (argumentTypes == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.argumentTypes = argumentTypes;
  }

  public @NotNull ResultNode getResult() {
    return result;
  }

  public void setResult(@NotNull ResultNode result) {
    if (result == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.result = result;
  }

  public @NotNull List<ThrowsSignatureNode> getThrowsSignatures() {
    return throwsSignatures;
  }

  public void setThrowsSignatures(@NotNull List<ThrowsSignatureNode> throwsSignatures) {
    if (throwsSignatures == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.throwsSignatures = throwsSignatures;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    StringBuilder sb = new StringBuilder();
    if (!typeParameters.isEmpty()) {
      sb.append('<');
      for (TypeParameterNode typeParameterNode : typeParameters) {
        sb.append(typeParameterNode.toString());
      }
      sb.append('>');
    }
    sb.append('(');
    for (TypeSignatureNode argumentType : argumentTypes) {
      sb.append(argumentType);
    }
    sb.append(')');
    sb.append(result.toString());
    for (ThrowsSignatureNode throwsSignatureNode : throwsSignatures) {
      sb.append(throwsSignatureNode.toString());
    }
    return sb.toString();
  }
}
