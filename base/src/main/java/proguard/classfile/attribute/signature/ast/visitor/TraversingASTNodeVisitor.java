package proguard.classfile.attribute.signature.ast.visitor;

import proguard.classfile.attribute.signature.ast.descriptor.ArrayTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.BaseTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.ClassTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.FieldDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.FieldTypeNode;
import proguard.classfile.attribute.signature.ast.descriptor.MethodDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.ReturnDescriptorNode;
import proguard.classfile.attribute.signature.ast.descriptor.VoidDescriptorNode;
import proguard.classfile.attribute.signature.ast.signature.AnyTypeArgumentNode;
import proguard.classfile.attribute.signature.ast.signature.ArrayTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.BoundedTypeArgumentNode;
import proguard.classfile.attribute.signature.ast.signature.ClassBoundNode;
import proguard.classfile.attribute.signature.ast.signature.ClassSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.ClassTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.InterfaceBoundNode;
import proguard.classfile.attribute.signature.ast.signature.MethodSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.PackageSpecifierNode;
import proguard.classfile.attribute.signature.ast.signature.ReferenceTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.ResultNode;
import proguard.classfile.attribute.signature.ast.signature.SimpleClassTypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.SuperclassSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.SuperinterfaceSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.ThrowsSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeArgumentNode;
import proguard.classfile.attribute.signature.ast.signature.TypeParameterNode;
import proguard.classfile.attribute.signature.ast.signature.TypeSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.TypeVariableSignatureNode;
import proguard.classfile.attribute.signature.ast.signature.WildcardIndicatorNode;

/**
 * A generic implementation of the {@link ASTNodeVisitor} intended to be used for when traversal of
 * the full AST is needed.
 *
 * @param <P> Parameter type for the visit methods
 */
public interface TraversingASTNodeVisitor<P> extends ASTNodeVisitor<Void, P> {
  @Override
  default Void visit(AnyTypeArgumentNode node, P arg) {
    return null;
  }

  @Override
  default Void visit(ArrayTypeSignatureNode node, P arg) {
    return node.getComponentType().accept(this, arg);
  }

  @Override
  default Void visit(BoundedTypeArgumentNode node, P arg) {
    return node.getReferenceTypeSignature().accept(this, arg);
  }

  @Override
  default Void visit(ClassBoundNode node, P arg) {
    ReferenceTypeSignatureNode referenceTypeSignature = node.getReferenceTypeSignature();
    if (referenceTypeSignature != null) {
      return referenceTypeSignature.accept(this, arg);
    } else {
      return null;
    }
  }

  @Override
  default Void visit(ClassSignatureNode node, P arg) {
    for (TypeParameterNode typeParameter : node.getTypeParameters()) {
      typeParameter.accept(this, arg);
    }
    node.getSuperclassSignature().accept(this, arg);
    for (SuperinterfaceSignatureNode superinterfaceSignature : node.getSuperinterfaceSignatures()) {
      superinterfaceSignature.accept(this, arg);
    }
    return null;
  }

  @Override
  default Void visit(ClassTypeSignatureNode node, P arg) {
    node.getPackageSpecifier().accept(this, arg);
    node.getName().accept(this, arg);
    for (SimpleClassTypeSignatureNode suffix : node.getSuffix()) {
      suffix.accept(this, arg);
    }
    return null;
  }

  @Override
  default Void visit(InterfaceBoundNode node, P arg) {
    return node.getInterfaceReference().accept(this, arg);
  }

  @Override
  default Void visit(TypeSignatureNode node, P arg) {
    if (node.getBaseType() != null) {
      return node.getBaseType().accept(this, arg);
    } else if (node.getReferenceTypeSignature() != null) {
      return node.getReferenceTypeSignature().accept(this, arg);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  default Void visit(MethodSignatureNode node, P arg) {
    for (TypeParameterNode typeParameter : node.getTypeParameters()) {
      typeParameter.accept(this, arg);
    }
    for (TypeSignatureNode argumentType : node.getArgumentTypes()) {
      argumentType.accept(this, arg);
    }
    node.getResult().accept(this, arg);
    for (ThrowsSignatureNode throwsSignature : node.getThrowsSignatures()) {
      throwsSignature.accept(this, arg);
    }
    return null;
  }

  @Override
  default Void visit(PackageSpecifierNode node, P arg) {
    return null;
  }

  @Override
  default Void visit(ReferenceTypeSignatureNode node, P arg) {
    if (node.getArrayType() != null) {
      return node.getArrayType().accept(this, arg);
    } else if (node.getTypeVariable() != null) {
      return node.getTypeVariable().accept(this, arg);
    } else if (node.getClassType() != null) {
      return node.getClassType().accept(this, arg);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  default Void visit(ResultNode node, P arg) {
    if (node.getJavaType() != null) {
      return node.getJavaType().accept(this, arg);
    } else {
      return null;
    }
  }

  @Override
  default Void visit(SimpleClassTypeSignatureNode node, P arg) {
    for (TypeArgumentNode typeArgument : node.getTypeArguments()) {
      typeArgument.accept(this, arg);
    }
    return null;
  }

  @Override
  default Void visit(SuperclassSignatureNode node, P arg) {
    return node.getClassType().accept(this, arg);
  }

  @Override
  default Void visit(SuperinterfaceSignatureNode node, P arg) {
    return node.getClassType().accept(this, arg);
  }

  @Override
  default Void visit(ThrowsSignatureNode node, P arg) {
    if (node.getClassType() != null) {
      return node.getClassType().accept(this, arg);
    } else {
      return null;
    }
  }

  @Override
  default Void visit(TypeArgumentNode node, P arg) {
    if (node.getBoundedTypeArg() != null) {
      return node.getBoundedTypeArg().accept(this, arg);
    } else {
      return null;
    }
  }

  @Override
  default Void visit(TypeParameterNode node, P arg) {
    node.getClassBound().accept(this, arg);
    for (InterfaceBoundNode interfaceBound : node.getInterfaceBounds()) {
      interfaceBound.accept(this, arg);
    }
    return null;
  }

  @Override
  default Void visit(TypeVariableSignatureNode node, P arg) {
    return null;
  }

  @Override
  default Void visit(WildcardIndicatorNode node, P arg) {
    return null;
  }

  @Override
  default Void visit(ArrayTypeNode node, P arg) {
    node.getComponentType().accept(this, arg);
    return null;
  }

  @Override
  default Void visit(BaseTypeNode node, P arg) {
    return null;
  }

  @Override
  default Void visit(ClassTypeNode node, P arg) {
    return null;
  }

  @Override
  default Void visit(FieldDescriptorNode node, P arg) {
    return node.getType().accept(this, arg);
  }

  @Override
  default Void visit(FieldTypeNode node, P arg) {
    if (node.getClassType() != null) {
      return node.getClassType().accept(this, arg);
    } else if (node.getArrayType() != null) {
      return node.getArrayType().accept(this, arg);
    } else if (node.getBaseType() != null) {
      return node.getBaseType().accept(this, arg);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  default Void visit(MethodDescriptorNode node, P arg) {
    for (FieldTypeNode parameter : node.getParameters()) {
      parameter.accept(this, arg);
    }
    node.getReturnDescriptor().accept(this, arg);
    return null;
  }

  @Override
  default Void visit(ReturnDescriptorNode node, P arg) {
    if (node.getFieldType() != null) {
      return node.getFieldType().accept(this, arg);
    } else {
      return null;
    }
  }

  @Override
  default Void visit(VoidDescriptorNode node, P arg) {
    return null;
  }
}
