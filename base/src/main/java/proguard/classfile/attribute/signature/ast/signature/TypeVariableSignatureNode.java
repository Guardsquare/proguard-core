package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class TypeVariableSignatureNode {
  private @NotNull String identifier;

  public TypeVariableSignatureNode(@NotNull String identifier) {
    if (identifier == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.identifier = identifier;
  }

  public @NotNull String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(@NotNull String identifier) {
    if (identifier == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.identifier = identifier;
  }

  @Override
  public @NotNull String toString() {
    return "T" + identifier + ";";
  }
}
