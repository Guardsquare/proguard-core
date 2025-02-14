package proguard.classfile.attribute.signature.ast.signature;

import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public enum WildcardIndicatorNode {
  PLUS("+"),
  MINUS("-");

  final String value;

  WildcardIndicatorNode(String value) {
    this.value = value;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return value;
  }
}
