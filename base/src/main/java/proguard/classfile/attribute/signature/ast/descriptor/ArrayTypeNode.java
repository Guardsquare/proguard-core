package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ArrayTypeNode {
  private @NotNull FieldTypeNode componentType;

  public ArrayTypeNode(@NotNull FieldTypeNode componentType) {
    if (componentType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }

    this.componentType = componentType;
  }

  public FieldTypeNode getComponentType() {
    return componentType;
  }

  public void setComponentType(@NotNull FieldTypeNode componentType) {
    if (componentType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }

    this.componentType = componentType;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return "[" + componentType.toString();
  }
}
