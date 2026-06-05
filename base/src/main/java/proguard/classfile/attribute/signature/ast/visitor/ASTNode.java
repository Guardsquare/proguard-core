package proguard.classfile.attribute.signature.ast.visitor;

public interface ASTNode {

  <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg);
}
