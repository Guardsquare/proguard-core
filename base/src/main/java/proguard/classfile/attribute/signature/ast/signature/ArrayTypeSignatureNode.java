package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ArrayTypeSignatureNode {
  private @NotNull JavaTypeSignatureNode componentType;

  public ArrayTypeSignatureNode(@NotNull JavaTypeSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.componentType = signature;
  }

  public @NotNull JavaTypeSignatureNode getComponentType() {
    return componentType;
  }

  public void setComponentType(@NotNull JavaTypeSignatureNode componentType) {
    if (componentType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.componentType = componentType;
  }

  @Override
  public @NotNull String toString() {
    return "[" + componentType.toString();
  }
}
