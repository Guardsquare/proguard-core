/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.classfile.kotlin.visitor.filter;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor;

import java.util.function.Predicate;

/**
 * Delegates to a given {@link KotlinAnnotationVisitor} if the predicate succeeds.
 *
 * @author James Hamilton
 */
public class KotlinAnnotationFilter
implements   KotlinAnnotationVisitor
{
    private final Predicate<KotlinMetadataAnnotation> predicate;
    private final KotlinAnnotationVisitor             acceptedKotlinAnnotationVisitor;
    private final KotlinAnnotationVisitor             rejectedKotlinAnnotationVisitor;


    public KotlinAnnotationFilter(Predicate<KotlinMetadataAnnotation> predicate,
                                  KotlinAnnotationVisitor             acceptedKotlinAnnotationVisitor)
    {
        this(predicate, acceptedKotlinAnnotationVisitor, null);
    }

    public KotlinAnnotationFilter(Predicate<KotlinMetadataAnnotation> predicate,
                                  KotlinAnnotationVisitor             acceptedKotlinAnnotationVisitor,
                                  KotlinAnnotationVisitor             rejectedKotlinAnnotationVisitor)
    {
        this.predicate                       = predicate;
        this.acceptedKotlinAnnotationVisitor = acceptedKotlinAnnotationVisitor;
        this.rejectedKotlinAnnotationVisitor = rejectedKotlinAnnotationVisitor;
    }

    @Override
    public void visitAnyAnnotation(Clazz clazz, KotlinMetadataAnnotation annotation) { }


    @Override
    public void visitTypeAnnotation(Clazz                    clazz,
                                    KotlinTypeMetadata       kotlinTypeMetadata,
                                    KotlinMetadataAnnotation annotation)
    {
        KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
        if (delegate != null)
        {
            annotation.accept(clazz, kotlinTypeMetadata, delegate);
        }
    }


    @Override
    public void visitTypeParameterAnnotation(Clazz                       clazz,
                                             KotlinTypeParameterMetadata kotlinTypeParameterMetadata,
                                             KotlinMetadataAnnotation    annotation)
    {
        KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
        if (delegate != null)
        {
            annotation.accept(clazz, kotlinTypeParameterMetadata, delegate);
        }
    }


    @Override
    public void visitTypeAliasAnnotation(Clazz                    clazz,
                                         KotlinTypeAliasMetadata  kotlinTypeAliasMetadata,
                                         KotlinMetadataAnnotation annotation)
    {
        KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
        if (delegate != null)
        {
            annotation.accept(clazz, kotlinTypeAliasMetadata, delegate);
        }
    }


    private KotlinAnnotationVisitor getDelegate(KotlinMetadataAnnotation kotlinMetadataAnnotation)
    {
        return this.predicate.test(kotlinMetadataAnnotation)
                ? this.acceptedKotlinAnnotationVisitor
                : this.rejectedKotlinAnnotationVisitor;
    }
}
