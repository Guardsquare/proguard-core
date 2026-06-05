package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class TypeVariableSignatureNode implements ASTNode {
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
  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    return "T" + identifier + ";";
  }
}
