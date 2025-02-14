package proguard.classfile.attribute.signature.ast.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.attribute.signature.ast.ASTStructureException;
import proguard.classfile.attribute.signature.ast.visitor.ASTNodeVisitor;

/**
 * @see proguard.classfile.attribute.signature.ast
 */
public class ReturnDescriptorNode {
  private @Nullable FieldTypeNode fieldType;
  private @Nullable VoidDescriptorNode voidDescriptor;

  public ReturnDescriptorNode(@NotNull FieldTypeNode fieldType) {
    if (fieldType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.fieldType = fieldType;
    this.voidDescriptor = null;
  }

  public ReturnDescriptorNode(@NotNull VoidDescriptorNode voidDescriptor) {
    if (voidDescriptor == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.fieldType = null;
    this.voidDescriptor = voidDescriptor;
  }

  public @Nullable FieldTypeNode getFieldType() {
    return fieldType;
  }

  public void changeToFieldType(@NotNull FieldTypeNode fieldType) {
    if (fieldType == null) {
      throw new ASTStructureException("Argument must not be null.");
    }
    this.fieldType = fieldType;
    this.voidDescriptor = null;
  }

  public boolean isVoid() {
    return this.voidDescriptor != null;
  }

  public void changeToVoid() {
    this.voidDescriptor = VoidDescriptorNode.INSTANCE;
    this.fieldType = null;
  }

  public <R, P> R accept(ASTNodeVisitor<R, P> visitor, P arg) {
    return visitor.visit(this, arg);
  }

  @Override
  public String toString() {
    if (this.fieldType != null) {
      return this.fieldType.toString();
    } else if (this.voidDescriptor != null) {
      return this.voidDescriptor.toString();
    } else {
      throw new ASTStructureException("At least one of the fields must be non-null");
    }
  }
}
