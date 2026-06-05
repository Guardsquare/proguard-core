package proguard.classfile.attribute.signature.ast.descriptor;

import proguard.classfile.attribute.signature.ast.visitor.ASTNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public enum VoidDescriptorNode implements ASTNode {
  INSTANCE;

  @Override
  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    return "V";
  }
}
