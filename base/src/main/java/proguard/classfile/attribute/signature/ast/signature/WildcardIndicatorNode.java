package proguard.classfile.attribute.signature.ast.signature;

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

  @Override
  public String toString() {
    return value;
  }
}
