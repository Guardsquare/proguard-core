/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
import proguard.classfile.kotlin.visitor.*;

import java.util.function.Predicate;

/**
 * This {@link KotlinTypeParameterFilter} delegates to another KotlinTypeVisitor if the
 * predicate succeeds.
 *
 * @author James Hamilton
 */
public class KotlinTypeParameterFilter
implements   KotlinTypeParameterVisitor
{
    private final Predicate<KotlinTypeParameterMetadata> predicate;
    private final KotlinTypeParameterVisitor             acceptedVisitor;
    private final KotlinTypeParameterVisitor             rejectedVisitor;

    public KotlinTypeParameterFilter(Predicate<KotlinTypeParameterMetadata> predicate,
                                     KotlinTypeParameterVisitor             acceptedVisitor,
                                     KotlinTypeParameterVisitor             rejectedVisitor)
    {
        this.predicate       = predicate;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }


    public KotlinTypeParameterFilter(Predicate<KotlinTypeParameterMetadata> predicate,
                                     KotlinTypeParameterVisitor             acceptedVisitor)
    {
        this(predicate, acceptedVisitor, null);
    }


    @Override
    public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata) { }


    @Override
    public void visitClassTypeParameter(Clazz                       clazz,
                                        KotlinClassKindMetadata     kotlinMetadata,
                                        KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        KotlinTypeParameterVisitor delegate = getDelegate(kotlinTypeParameterMetadata);

        if (delegate != null)
        {
            kotlinTypeParameterMetadata.accept(clazz, kotlinMetadata, delegate);
        }
    }


    @Override
    public void visitPropertyTypeParameter(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinPropertyMetadata             kotlinPropertyMetadata,
                                           KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
    {
        KotlinTypeParameterVisitor delegate = getDelegate(kotlinTypeParameterMetadata);

        if (delegate != null)
        {
            kotlinTypeParameterMetadata.accept(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata, delegate);
        }
    }


    @Override
    public void visitFunctionTypeParameter(Clazz                       clazz,
                                           KotlinMetadata              kotlinMetadata,
                                           KotlinFunctionMetadata      kotlinFunctionMetadata,
                                           KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        KotlinTypeParameterVisitor delegate = getDelegate(kotlinTypeParameterMetadata);

        if (delegate != null)
        {
            kotlinTypeParameterMetadata.accept(clazz, kotlinMetadata, kotlinFunctionMetadata, delegate);
        }
    }


    @Override
    public void visitAliasTypeParameter(Clazz                              clazz,
                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                        KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                        KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
    {
        KotlinTypeParameterVisitor delegate = getDelegate(kotlinTypeParameterMetadata);

        if (delegate != null)
        {
            kotlinTypeParameterMetadata.accept(clazz, kotlinDeclarationContainerMetadata, kotlinTypeAliasMetadata, delegate);
        }
    }


    private KotlinTypeParameterVisitor getDelegate(KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        return this.predicate.test(kotlinTypeParameterMetadata) ?
                this.acceptedVisitor : this.rejectedVisitor;
    }
}
