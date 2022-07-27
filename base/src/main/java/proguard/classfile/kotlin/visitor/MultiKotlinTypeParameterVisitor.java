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

package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;

/**
 * This {@link KotlinTypeVisitor} delegates to multiple {@link KotlinTypeVisitor}s.
 *
 * @author James Hamilton
 */
public class MultiKotlinTypeParameterVisitor
implements   KotlinTypeParameterVisitor
{
    private final KotlinTypeParameterVisitor[] kotlinTypeParameterVisitors;

    public MultiKotlinTypeParameterVisitor(KotlinTypeParameterVisitor...kotlinTypeParameterVisitor)
    {
        this.kotlinTypeParameterVisitors = kotlinTypeParameterVisitor;
    }


    @Override
    public void visitAnyTypeParameter(Clazz clazz, KotlinTypeParameterMetadata kotlinTypeParameterMetadata) { }


    @Override
    public void visitClassTypeParameter(Clazz                       clazz,
                                        KotlinClassKindMetadata     kotlinClassKindMetadata,
                                        KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitClassTypeParameter(clazz, kotlinClassKindMetadata, kotlinTypeParameterMetadata);
        }
    }


    @Override
    public void visitPropertyTypeParameter(Clazz                              clazz,
                                           KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                           KotlinPropertyMetadata             kotlinPropertyMetadata,
                                           KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitPropertyTypeParameter(clazz,
                                                                  kotlinDeclarationContainerMetadata,
                                                                  kotlinPropertyMetadata,
                                                                  kotlinTypeParameterMetadata);
        }
    }


    @Override
    public void visitFunctionTypeParameter(Clazz                       clazz,
                                           KotlinMetadata              kotlinMetadata,
                                           KotlinFunctionMetadata      kotlinFunctionMetadata,
                                           KotlinTypeParameterMetadata kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitFunctionTypeParameter(clazz,
                                                                  kotlinMetadata,
                                                                  kotlinFunctionMetadata,
                                                                  kotlinTypeParameterMetadata);
        }
    }


    @Override
    public void visitAliasTypeParameter(Clazz                              clazz,
                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                        KotlinTypeAliasMetadata            kotlinTypeAliasMetadata,
                                        KotlinTypeParameterMetadata        kotlinTypeParameterMetadata)
    {
        for (KotlinTypeParameterVisitor kotlinTypeParameterVisitor : this.kotlinTypeParameterVisitors)
        {
            kotlinTypeParameterVisitor.visitAliasTypeParameter(clazz,
                                                               kotlinDeclarationContainerMetadata,
                                                               kotlinTypeAliasMetadata,
                                                               kotlinTypeParameterMetadata);
        }
    }
}
