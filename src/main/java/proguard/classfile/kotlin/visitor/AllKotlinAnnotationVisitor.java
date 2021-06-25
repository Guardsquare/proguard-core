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
    public void visitKotlinDeclarationContainerMetadata(Clazz clazz, KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        kotlinDeclarationContainerMetadata.typeAliasesAccept(clazz, this);

        visitAnyKotlinMetadata(clazz, kotlinDeclarationContainerMetadata);
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
