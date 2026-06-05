package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ClassTypeNode implements ASTNode {
  private @NotNull String classname;

  public ClassTypeNode(@NotNull String classname) {
    if (classname == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.classname = classname;
  }

  public @NotNull String getClassname() {
    return classname;
  }

  public void setClassname(@NotNull String classname) {
    this.classname = classname;
  }

  @Override
  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return "L" + classname + ";";
  }
}
