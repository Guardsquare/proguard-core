package proguard.classfile.attribute.signature.ast.signature;

import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class TypeParameterNode {
  private @NotNull String identifier;
  private @NotNull ClassBoundNode classBound;
  private @NotNull List<InterfaceBoundNode> interfaceBounds;

  public TypeParameterNode(
      @NotNull String identifier,
      @NotNull ClassBoundNode classBound,
      @NotNull List<InterfaceBoundNode> interfaceBounds) {
    if (identifier == null || classBound == null || interfaceBounds == null) {
      throw new ASTStructureException("Arguments must not be null");
    }
    this.identifier = identifier;
    this.classBound = classBound;
    this.interfaceBounds = interfaceBounds;
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

  public @NotNull ClassBoundNode getClassBound() {
    return classBound;
  }

  public void setClassBound(@NotNull ClassBoundNode classBound) {
    if (classBound == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classBound = classBound;
  }

  public @NotNull List<InterfaceBoundNode> getInterfaceBounds() {
    return interfaceBounds;
  }

  public void setInterfaceBounds(@NotNull List<InterfaceBoundNode> interfaceBounds) {
    if (interfaceBounds == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.interfaceBounds = interfaceBounds;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    return identifier
        + classBound.toString()
        + interfaceBounds.stream().map(Object::toString).collect(Collectors.joining());
  }
}
