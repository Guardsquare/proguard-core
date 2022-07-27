/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private final Predicate<KotlinAnnotation> predicate;
    private final KotlinAnnotationVisitor     acceptedKotlinAnnotationVisitor;
    private final KotlinAnnotationVisitor     rejectedKotlinAnnotationVisitor;


    public KotlinAnnotationFilter(Predicate<KotlinAnnotation> predicate,
                                  KotlinAnnotationVisitor     acceptedKotlinAnnotationVisitor)
    {
        this(predicate, acceptedKotlinAnnotationVisitor, null);
    }

    public KotlinAnnotationFilter(Predicate<KotlinAnnotation> predicate,
                                  KotlinAnnotationVisitor     acceptedKotlinAnnotationVisitor,
                                  KotlinAnnotationVisitor     rejectedKotlinAnnotationVisitor)
    {
        this.predicate                       = predicate;
        this.acceptedKotlinAnnotationVisitor = acceptedKotlinAnnotationVisitor;
        this.rejectedKotlinAnnotationVisitor = rejectedKotlinAnnotationVisitor;
    }

    @Override
    public void visitAnyAnnotation(Clazz clazz, KotlinAnnotatable annotatable, KotlinAnnotation annotation) { }


    @Override
    public void visitTypeAnnotation(Clazz              clazz,
                                    KotlinTypeMetadata kotlinTypeMetadata,
                                    KotlinAnnotation   annotation)
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
                                             KotlinAnnotation            annotation)
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
                                         KotlinAnnotation         annotation)
    {
        KotlinAnnotationVisitor delegate = this.getDelegate(annotation);
        if (delegate != null)
        {
            annotation.accept(clazz, kotlinTypeAliasMetadata, delegate);
        }
    }


    private KotlinAnnotationVisitor getDelegate(KotlinAnnotation kotlinMetadataAnnotation)
    {
        return this.predicate.test(kotlinMetadataAnnotation)
                ? this.acceptedKotlinAnnotationVisitor
                : this.rejectedKotlinAnnotationVisitor;
    }
}
