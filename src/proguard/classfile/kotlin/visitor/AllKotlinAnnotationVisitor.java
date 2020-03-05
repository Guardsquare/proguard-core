/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;

public class AllKotlinAnnotationVisitor
implements   KotlinMetadataVisitor,
             KotlinTypeAliasVisitor,
             KotlinTypeParameterVisitor,
             KotlinTypeVisitor
{
    private final KotlinAnnotationVisitor delegate;

    public AllKotlinAnnotationVisitor(KotlinAnnotationVisitor delegate)
    {
        this.delegate = delegate;
    }


    // Implementations for KotlinMetadataVisitor.

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new AllTypeVisitor(         this));
        kotlinMetadata.accept(clazz, new AllTypeParameterVisitor(this));
    }

    @Override
    public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
    {
        kotlinClassKindMetadata.typeAliasesAccept(clazz, this);

        visitAnyKotlinMetadata(clazz, kotlinClassKindMetadata);
    }


    // Implementations for KotlinTypeVisitor.

    @Override
    public void visitAnyType(Clazz clazz, KotlinTypeMetadata kotlinTypeMetadata)
    {
        kotlinTypeMetadata.annotationsAccept(clazz, delegate);
    }

    // Implementations for KotlinTypeAliasVisitor.

    @Override
    public void visitTypeAlias(Clazz                              clazz,
                               KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                               KotlinTypeAliasMetadata            kotlinTypeAliasMetadata)
    {
        kotlinTypeAliasMetadata.annotationsAccept(clazz, delegate);
    }


    // Implementations for KotlinTypeParameterVisitor.

    @Override
    public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        kotlinTypeParameterMetadata.annotationsAccept(clazz, delegate);
    }
}