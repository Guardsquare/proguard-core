package proguard.classfile.attribute.signature.ast.visitor;

import java.util.Collection;
import proguard.classfile.attribute.signature.ast.descriptor.ClassTypeNode;
import proguard.classfile.attribute.signature.ast.signature.ClassTypeSignatureNode;

/**
 * A signature/descriptor visitor that adds all class names into a given collection.
 *
 * <p>Can be used to partially replace {@link proguard.classfile.util.DescriptorClassEnumeration} or
 * {@link proguard.classfile.util.InternalTypeEnumeration}
 */
public class ClassNameCollectingVisitor implements TraversingASTNodeVisitor<Collection<String>> {
  @Override
  public Void visit(ClassTypeSignatureNode node, Collection<String> arg) {
    arg.addAll(node.getClassNamesIncludingParentClasses());
    return TraversingASTNodeVisitor.super.visit(node, arg);
  }

  @Override
  public Void visit(ClassTypeNode node, Collection<String> arg) {
    arg.add(node.getClassname());
    return TraversingASTNodeVisitor.super.visit(node, arg);
  }
}
