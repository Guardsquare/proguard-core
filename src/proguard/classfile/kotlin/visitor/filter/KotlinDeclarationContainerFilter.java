/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

import java.util.function.Predicate;

/**
 * Filter KotlinDeclarationContainers, based on the given predicate.
 *
 * @author James Hamilton
 */
public class KotlinDeclarationContainerFilter
implements   KotlinMetadataVisitor
{
    private final Predicate<KotlinDeclarationContainerMetadata> predicate;
    private final KotlinMetadataVisitor                         acceptedVisitor;
    private final KotlinMetadataVisitor                         rejectedVisitor;


    public KotlinDeclarationContainerFilter(KotlinMetadataVisitor acceptedVisitor)
    {
        this(__ -> true, acceptedVisitor, null);
    }

    public KotlinDeclarationContainerFilter(Predicate<KotlinDeclarationContainerMetadata> predicate,
                                            KotlinMetadataVisitor                         acceptedVisitor)
    {
        this(predicate, acceptedVisitor, null);
    }

    public KotlinDeclarationContainerFilter(Predicate<KotlinDeclarationContainerMetadata> predicate,
                                            KotlinMetadataVisitor                         acceptedVisitor,
                                            KotlinMetadataVisitor                         rejectedVisitor)
    {
        this.predicate       = predicate;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        KotlinMetadataVisitor visitor = this.predicate.test(kotlinDeclarationContainerMetadata) ? acceptedVisitor : rejectedVisitor;
        if (visitor != null)
        {
            visitor.visitKotlinDeclarationContainerMetadata(clazz, kotlinDeclarationContainerMetadata);
        }
    }
}
