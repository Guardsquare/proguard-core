/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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
package proguard.util.kotlin.asserter.constraint;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

/**
 */
public class DeclarationContainerIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinMetadataVisitor
{
    // Implementations for KotlinMetadataVisitor.

    @Override
    public void visitKotlinDeclarationContainerMetadata(Clazz clazz,
                                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        if (kotlinDeclarationContainerMetadata.ownerClassName != null)
        {
            AssertUtil assertUtil = new AssertUtil("Declaration container " +
                                                   kotlinDeclarationContainerMetadata.ownerClassName,
                                                   reporter,
                                                   programClassPool,
                                                   libraryClassPool);
            assertUtil
                .reportIfNullReference("referenced owner class", kotlinDeclarationContainerMetadata.ownerReferencedClass);
            assertUtil
                .reportIfClassDangling("referenced owner class", kotlinDeclarationContainerMetadata.ownerReferencedClass);
        }
    }
}