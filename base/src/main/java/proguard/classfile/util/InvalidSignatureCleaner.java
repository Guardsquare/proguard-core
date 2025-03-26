package proguard.classfile.util;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.RecordComponentInfo;
import proguard.classfile.attribute.SignatureAttribute;
import proguard.classfile.attribute.signature.ast.signature.ClassSignatureNode;
import proguard.classfile.attribute.signature.grammars.ClassSignatureGrammar;
import proguard.classfile.attribute.signature.grammars.MethodSignatureGrammar;
import proguard.classfile.attribute.signature.grammars.TypeSignatureGrammar;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.editor.NamedAttributeDeleter;

/** A small utility class to clean up invalid signatures. */
public class InvalidSignatureCleaner implements AttributeVisitor {
  /**
   * Checks whether the given class signature corresponds to the source of truth registered in the
   * given clazz's bytecode.
   */
  public static boolean isValidClassSignature(Clazz clazz, ClassSignatureNode signature) {
    // Check superclass.
    if (!signature
        .getSuperclassSignature()
        .getClassType()
        .getClassname()
        .equals(clazz.getSuperName())) {
      return false;
    }

    // Check interfaces.
    if (signature.getSuperinterfaceSignatures().size() != clazz.getInterfaceCount()) {
      return false;
    }

    Set<String> clazzInterfaces =
        IntStream.range(0, clazz.getInterfaceCount())
            .mapToObj(clazz::getInterfaceName)
            .collect(Collectors.toSet());
    Set<String> signatureInterfaces =
        signature.getSuperinterfaceSignatures().stream()
            .map(sig -> sig.getClassType().getClassname())
            .collect(Collectors.toSet());
    return clazzInterfaces.containsAll(signatureInterfaces)
        && signatureInterfaces.containsAll(clazzInterfaces);
  }

  // Implementations for AttributeVisitor.

  @Override
  public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

  @Override
  public void visitSignatureAttribute(Clazz clazz, SignatureAttribute attribute) {
    ClassSignatureNode signature = ClassSignatureGrammar.parse(attribute.getSignature(clazz));
    if (signature == null || !isValidClassSignature(clazz, signature)) {
      // The signature cannot be parsed or is invalid.
      clazz.accept(new NamedAttributeDeleter(Attribute.SIGNATURE));
    }
  }

  @Override
  public void visitSignatureAttribute(
      Clazz clazz, RecordComponentInfo recordComponentInfo, SignatureAttribute attribute) {
    if (recordComponentInfo.referencedField != null) {
      if (TypeSignatureGrammar.parse(attribute.getSignature(clazz)) == null) {
        recordComponentInfo.attributesAccept(clazz, new NamedAttributeDeleter(Attribute.SIGNATURE));
      }
    }
  }

  @Override
  public void visitSignatureAttribute(Clazz clazz, Member member, SignatureAttribute attribute) {
    boolean invalid = false;
    if (member instanceof Field) {
      invalid = TypeSignatureGrammar.parse(attribute.getSignature(clazz)) == null;
    } else if (member instanceof Method) {
      invalid = MethodSignatureGrammar.parse(attribute.getSignature(clazz)) == null;
    }

    if (invalid) {
      member.accept(clazz, new NamedAttributeDeleter(Attribute.SIGNATURE));
    }
  }
}
