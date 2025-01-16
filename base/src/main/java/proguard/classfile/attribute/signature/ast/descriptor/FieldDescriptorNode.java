package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

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

  @Override
  public String toString() {
    return type.toString();
  }
}
