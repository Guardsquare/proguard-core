package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ArrayTypeSignatureNode {
  private @NotNull TypeSignatureNode componentType;

  public ArrayTypeSignatureNode(@NotNull TypeSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.componentType = signature;
  }

  public @NotNull TypeSignatureNode getComponentType() {
    return componentType;
  }

  public void setComponentType(@NotNull TypeSignatureNode componentType) {
    if (componentType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.componentType = componentType;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    return "[" + componentType.toString();
  }
}
