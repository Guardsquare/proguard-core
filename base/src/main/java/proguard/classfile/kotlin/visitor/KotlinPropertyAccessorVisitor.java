package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorMetadata;

public interface KotlinPropertyAccessorVisitor {
  void visitAnyPropertyAccessor(
      Clazz clazz,
      KotlinMetadata kotlinMetadata,
      KotlinPropertyMetadata kotlinPropertyMetadata,
      KotlinPropertyAccessorMetadata kotlinPropertyAccessorMetadata);
}
