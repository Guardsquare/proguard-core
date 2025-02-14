package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class SuperclassSignatureNode {
  private @NotNull ClassTypeSignatureNode classType;

  public SuperclassSignatureNode(@NotNull ClassTypeSignatureNode classTypeSignature) {
    if (classTypeSignature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = classTypeSignature;
  }

  public @NotNull ClassTypeSignatureNode getClassType() {
    return classType;
  }

  public void setClassType(@NotNull ClassTypeSignatureNode classType) {
    if (classType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classType = classType;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return classType.toString();
  }
}
