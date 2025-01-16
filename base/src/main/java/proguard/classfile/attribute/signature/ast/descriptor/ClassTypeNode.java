package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ClassTypeNode {
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
  public String toString() {
    return "L" + classname + ";";
  }
}
