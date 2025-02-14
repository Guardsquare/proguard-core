package proguard.classfile.attribute.signature.ast.signature;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

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

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  /**
   * @return the internal class name in the descriptor (same as the file would be named in the JAR
   *     file)
   */
  public @NotNull String getClassname() {
    String suffixString = "";
    if (!suffix.isEmpty()) {
      suffixString =
          "$"
              + suffix.stream()
                  .map(SimpleClassTypeSignatureNode::getIdentifier)
                  .collect(Collectors.joining("$"));
    }
    return packageSpecifier.toString() + name.getIdentifier() + suffixString;
  }

  /**
   * @return a list of the internal class names in the descriptor for this class and every parent
   *     class if nested
   */
  public @NotNull List<String> getClassNamesIncludingParentClasses() {
    List<String> result = new ArrayList<>(suffix.size() + 1);
    result.add(packageSpecifier + name.getIdentifier());

    for (int index = 1; index < suffix.size() + 1; index++) {
      result.add(
          packageSpecifier
              + name.getIdentifier()
              + "$"
              + suffix.stream()
                  .limit(index)
                  .map(SimpleClassTypeSignatureNode::getIdentifier)
                  .collect(Collectors.joining("$")));
    }

    return result;
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
