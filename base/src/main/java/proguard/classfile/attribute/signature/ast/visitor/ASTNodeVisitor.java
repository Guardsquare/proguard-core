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
 * A visitor of grammar and signature AST nodes, in the style of the visitor design pattern.
 *
 * <p><b>WARNING:</b> It is possible that methods will be added to this interface to accommodate
 * new, currently unknown, language structures added to future versions of the JVM. Visitor classes
 * directly implementing this interface may be source incompatible with future versions of the
 * library/platform.
 *
 * @param <R> the return type of this visitor's methods. Use {@link Void} for visitors that do not
 *     need to return results.
 * @param <P> the type of the additional parameter to this visitor's methods. Use {@link Void} for
 *     visitors that do not need an additional parameter.
 */
public interface ASTNodeVisitor<R, P> {
  R visit(AnyTypeArgumentNode node, P arg);

  R visit(ArrayTypeSignatureNode node, P arg);

  R visit(BoundedTypeArgumentNode node, P arg);

  R visit(ClassBoundNode node, P arg);

  R visit(ClassSignatureNode node, P arg);

  R visit(ClassTypeSignatureNode node, P arg);

  R visit(InterfaceBoundNode node, P arg);

  R visit(TypeSignatureNode node, P arg);

  R visit(MethodSignatureNode node, P arg);

  R visit(PackageSpecifierNode node, P arg);

  R visit(ReferenceTypeSignatureNode node, P arg);

  R visit(ResultNode node, P arg);

  R visit(SimpleClassTypeSignatureNode node, P arg);

  R visit(SuperclassSignatureNode node, P arg);

  R visit(SuperinterfaceSignatureNode node, P arg);

  R visit(ThrowsSignatureNode node, P arg);

  R visit(TypeArgumentNode node, P arg);

  R visit(TypeParameterNode node, P arg);

  R visit(TypeVariableSignatureNode node, P arg);

  R visit(WildcardIndicatorNode node, P arg);

  R visit(ArrayTypeNode node, P arg);

  R visit(BaseTypeNode node, P arg);

  R visit(ClassTypeNode node, P arg);

  R visit(FieldDescriptorNode node, P arg);

  R visit(FieldTypeNode node, P arg);

  R visit(MethodDescriptorNode node, P arg);

  R visit(ReturnDescriptorNode node, P arg);

  R visit(VoidDescriptorNode node, P arg);
}
