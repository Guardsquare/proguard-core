package proguard.classfile.attribute.signature.ast.descriptor;

import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class MethodDescriptorNode {
  private @NotNull List<FieldTypeNode> parameters;
  private @NotNull ReturnDescriptorNode returnDescriptor;

  public MethodDescriptorNode(
      @NotNull List<FieldTypeNode> parameters, @NotNull ReturnDescriptorNode returnDescriptor) {
    this.parameters = parameters;
    this.returnDescriptor = returnDescriptor;
  }

  public @NotNull List<FieldTypeNode> getParameters() {
    return parameters;
  }

  public void setParameters(@NotNull List<FieldTypeNode> parameters) {
    if (parameters == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.parameters = parameters;
  }

  public @NotNull ReturnDescriptorNode getReturnDescriptor() {
    return returnDescriptor;
  }

  public void setReturnDescriptor(@NotNull ReturnDescriptorNode returnDescriptor) {
    if (returnDescriptor == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.returnDescriptor = returnDescriptor;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return "("
        + parameters.stream().map(Object::toString).collect(Collectors.joining())
        + ")"
        + returnDescriptor.toString();
  }
}
