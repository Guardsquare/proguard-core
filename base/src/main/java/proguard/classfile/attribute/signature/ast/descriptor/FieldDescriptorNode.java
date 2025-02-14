package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class FieldDescriptorNode {
  private @NotNull FieldTypeNode type;

  public FieldDescriptorNode(@NotNull FieldTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.type = type;
  }

  public @NotNull FieldTypeNode getType() {
    return type;
  }

  public void setType(@NotNull FieldTypeNode type) {
    if (type == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.type = type;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
