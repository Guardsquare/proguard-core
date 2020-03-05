/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor;

import java.util.function.Predicate;

/**
 * Delegate to another {@link KotlinPropertyVisitor} if the predicate returns true.
 */
public class KotlinPropertyFilter
implements   KotlinPropertyVisitor
{
    private final KotlinPropertyVisitor             acceptedPropertyVisitor;
    private final KotlinPropertyVisitor             rejectedPropertyVisitor;
    private final Predicate<KotlinPropertyMetadata> predicate;


    public KotlinPropertyFilter(Predicate<KotlinPropertyMetadata> predicate,
                                KotlinPropertyVisitor             acceptedPropertyVisitor,
                                KotlinPropertyVisitor             rejectedPropertyVisitor)
    {
        this.acceptedPropertyVisitor = acceptedPropertyVisitor;
        this.rejectedPropertyVisitor = rejectedPropertyVisitor;
        this.predicate               = predicate;
    }

    public KotlinPropertyFilter(Predicate<KotlinPropertyMetadata> predicate,
                                KotlinPropertyVisitor             acceptedPropertyVisitor)
    {
        this(predicate, acceptedPropertyVisitor, null);
    }

    // Implementations for KotlinPropertyVisitor.

    @Override
    public void visitAnyProperty(Clazz                              clazz,
                                 KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                 KotlinPropertyMetadata             kotlinPropertyMetadata)
    {
        KotlinPropertyVisitor visitor = this.predicate.test(kotlinPropertyMetadata) ? acceptedPropertyVisitor : rejectedPropertyVisitor;
        if (visitor != null)
        {
            visitor.visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
        }
    }
}
