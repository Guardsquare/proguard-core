package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinMetadata;

/**
 * This {@link KotlinMetadataVisitor} lets a given {@link KotlinEnumEntryVisitor} visit all enum
 * entries of visited {@link KotlinClassKindMetadata}.
 */
public class AllEnumEntryVisitor implements KotlinMetadataVisitor {

  private final KotlinEnumEntryVisitor delegateEnumEntryVisitor;

  public AllEnumEntryVisitor(KotlinEnumEntryVisitor kotlinEnumEntryVisitor) {
    this.delegateEnumEntryVisitor = kotlinEnumEntryVisitor;
  }

  @Override
  public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

  @Override
  public void visitKotlinClassMetadata(
      Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata) {
    kotlinClassKindMetadata.enumEntriesAccept(clazz, delegateEnumEntryVisitor);
  }
}
