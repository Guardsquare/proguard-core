/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */
package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

import java.util.function.Predicate;

/**
 * Delegate to another {@link KotlinMetadataVisitor} if the predicate returns true.
 *
 */
public class KotlinMultiFilePartKindFilter
implements   KotlinMetadataVisitor
{
    private final Predicate<KotlinMultiFilePartKindMetadata> predicate;
    private final KotlinMetadataVisitor                      kotlinMetadataVisitor;

    public KotlinMultiFilePartKindFilter(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this(__ -> true, kotlinMetadataVisitor);
    }

    public KotlinMultiFilePartKindFilter(Predicate<KotlinMultiFilePartKindMetadata> predicate,
                                         KotlinMetadataVisitor                      kotlinMetadataVisitor)
    {
        this.predicate             = predicate;
        this.kotlinMetadataVisitor = kotlinMetadataVisitor;
    }


    @Override
    public void visitKotlinMultiFilePartMetadata(Clazz                           clazz,
                                                 KotlinMultiFilePartKindMetadata kotlinMultiFilePartKindMetadata)
    {
        if (this.predicate.test(kotlinMultiFilePartKindMetadata))
        {
            kotlinMultiFilePartKindMetadata.accept(clazz, this.kotlinMetadataVisitor);
        }
    }


    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}
}
