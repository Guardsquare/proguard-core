package proguard.classfile.attribute.signature.ast.signature;

import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ClassTypeSignatureNode {
  private @NotNull PackageSpecifierNode packageSpecifier;
  private @NotNull SimpleClassTypeSignatureNode name;
  private @NotNull List<SimpleClassTypeSignatureNode> suffix;

  public ClassTypeSignatureNode(
      @NotNull PackageSpecifierNode packageSpecifier,
      @NotNull SimpleClassTypeSignatureNode name,
      @NotNull List<SimpleClassTypeSignatureNode> suffix) {
    if (packageSpecifier == null || name == null || suffix == null) {
      throw new ASTStructureException("Arguments must not be null.");
    }
    this.packageSpecifier = packageSpecifier;
    this.name = name;
    this.suffix = suffix;
  }

  public @NotNull PackageSpecifierNode getPackageSpecifier() {
    return packageSpecifier;
  }

  public void setPackageSpecifier(@NotNull PackageSpecifierNode packageSpecifier) {
    if (packageSpecifier == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.packageSpecifier = packageSpecifier;
  }

  public @NotNull SimpleClassTypeSignatureNode getName() {
    return name;
  }

  public void setName(@NotNull SimpleClassTypeSignatureNode name) {
    if (name == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.name = name;
  }

  public @NotNull List<SimpleClassTypeSignatureNode> getSuffix() {
    return suffix;
  }

  public void setSuffix(@NotNull List<SimpleClassTypeSignatureNode> suffix) {
    if (suffix == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.suffix = suffix;
  }

  @Override
  public @NotNull String toString() {
    @NotNull String suffixString = "";
    if (!suffix.isEmpty()) {
      suffixString =
          "."
              + suffix.stream()
                  .map(SimpleClassTypeSignatureNode::toString)
                  .collect(Collectors.joining("."));
    }

    return "L" + packageSpecifier + name + suffixString + ";";
  }
}
