package proguard.classfile.attribute.signature.ast.signature;

import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class SimpleClassTypeSignatureNode {
  private @NotNull String identifier;
  private @NotNull List<TypeArgumentNode> typeArguments;

  public SimpleClassTypeSignatureNode(
      @NotNull String identifier, @NotNull List<TypeArgumentNode> typeArguments) {
    if (identifier == null || typeArguments == null) {
      throw new ASTStructureException("Arguments must not be null.");
    }
    this.identifier = identifier;
    this.typeArguments = typeArguments;
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

  public @NotNull List<TypeArgumentNode> getTypeArguments() {
    return typeArguments;
  }

  public void setTypeArguments(@NotNull List<TypeArgumentNode> typeArguments) {
    if (typeArguments == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.typeArguments = typeArguments;
  }

  @Override
  public @NotNull String toString() {
    return identifier
        + (typeArguments.isEmpty()
            ? ""
            : "<"
                + typeArguments.stream()
                    .map(TypeArgumentNode::toString)
                    .collect(Collectors.joining())
                + ">");
  }
}
