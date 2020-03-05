/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */
package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor;

import java.util.function.Predicate;


public class KotlinConstructorFilter
implements   KotlinConstructorVisitor
{
    private final KotlinConstructorVisitor             kotlinConstructorVisitor;
    private final Predicate<KotlinConstructorMetadata> predicate;

    public KotlinConstructorFilter(Predicate<KotlinConstructorMetadata> predicate,
                                   KotlinConstructorVisitor             kotlinConstructorVisitor)
    {
        this.kotlinConstructorVisitor = kotlinConstructorVisitor;
        this.predicate                = predicate;
    }


    // Implementations for KotlinConstructorFilter.
    @Override
    public void visitConstructor(Clazz clazz,
                                 KotlinClassKindMetadata kotlinClassKindMetadata,
                                 KotlinConstructorMetadata kotlinConstructorMetadata)
    {
        if (predicate.test(kotlinConstructorMetadata))
        {
            kotlinConstructorMetadata.accept(clazz, kotlinClassKindMetadata, kotlinConstructorVisitor);
        }
    }
}
