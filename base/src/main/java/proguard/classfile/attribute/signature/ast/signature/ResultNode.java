package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.descriptor.VoidDescriptorNode;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ResultNode {
  private @Nullable TypeSignatureNode javaType;
  private @Nullable VoidDescriptorNode voidDescriptor;

  public ResultNode(@NotNull VoidDescriptorNode voidDescriptor) {
    if (voidDescriptor == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.javaType = null;
    this.voidDescriptor = voidDescriptor;
  }

  public ResultNode(@NotNull TypeSignatureNode signature) {
    if (signature == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.javaType = signature;
    this.voidDescriptor = null;
  }

  public boolean isVoid() {
    return this.voidDescriptor != null;
  }

  public void changeToVoid() {
    this.voidDescriptor = VoidDescriptorNode.INSTANCE;
    this.javaType = null;
  }

  public @Nullable TypeSignatureNode getJavaType() {
    return javaType;
  }

  public void changeToJavaType(@NotNull TypeSignatureNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.javaType = node;
    this.voidDescriptor = null;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    if (voidDescriptor != null) {
      return voidDescriptor.toString();
    } else if (javaType != null) {
      return javaType.toString();
    } else {
      throw new ASTStructureException("Both fields can't be null at once.");
    }
  }
}
