package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinEnumEntryMetadata;

public interface KotlinEnumEntryVisitor {
  void visitAnyEnumEntry(
      Clazz clazz,
      KotlinClassKindMetadata kotlinClassKindMetadata,
      KotlinEnumEntryMetadata kotlinEnumEntryMetadata);
}
