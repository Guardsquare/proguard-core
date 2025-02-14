package proguard.classfile.attribute.signature.ast.signature;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class PackageSpecifierNode {
  private @NotNull List<String> packageNames;

  public PackageSpecifierNode(@NotNull List<String> packageNames) {
    if (packageNames == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.packageNames = packageNames;
  }

  public PackageSpecifierNode() {
    this.packageNames = Collections.emptyList();
  }

  public @NotNull List<String> getPackageNames() {
    return packageNames;
  }

  public void setPackageNames(@NotNull List<String> packageNames) {
    if (packageNames == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.packageNames = packageNames;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public @NotNull String toString() {
    if (packageNames.isEmpty()) {
      return "";
    } else {
      return String.join("/", packageNames) + "/";
    }
  }
}
