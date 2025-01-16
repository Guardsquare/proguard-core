package proguard.classfile.attribute.signature.ast.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.descriptor.VoidDescriptorNode;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ResultNode {
  private @Nullable JavaTypeSignatureNode javaType;
  private @Nullable VoidDescriptorNode voidDescriptor;

  public ResultNode(@NotNull VoidDescriptorNode voidDescriptor) {
    if (voidDescriptor == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.javaType = null;
    this.voidDescriptor = voidDescriptor;
  }

  public ResultNode(@NotNull JavaTypeSignatureNode signature) {
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

  public @Nullable JavaTypeSignatureNode getJavaType() {
    return javaType;
  }

  public void changeToJavaType(@NotNull JavaTypeSignatureNode node) {
    if (node == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.javaType = node;
    this.voidDescriptor = null;
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
