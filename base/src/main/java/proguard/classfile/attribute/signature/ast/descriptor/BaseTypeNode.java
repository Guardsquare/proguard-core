package proguard.classfile.attribute.signature.ast.descriptor;

import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public enum BaseTypeNode {
  B,
  C,
  D,
  F,
  I,
  J,
  S,
  Z;

  /** Return true if this base type corresponds to a category 2 value. */
  public boolean isCategory2() {
    return (this == J || this == D);
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }
}
