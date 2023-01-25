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
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinConstructorMetadata;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.KotlinValueParameterMetadata;
import proguard.classfile.kotlin.visitor.AllValueParameterVisitor;
import proguard.classfile.kotlin.visitor.KotlinValueParameterVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

/**
 */
public class ValueParameterIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinValueParameterVisitor
{

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new AllValueParameterVisitor(this));
    }


    // Implementations for KotlinValueParameterVisitor.
    @Override
    public void visitAnyValueParameter(Clazz                        clazz,
                                       KotlinValueParameterMetadata kotlinValueParameterMetadata) {}
    @Override
    public void visitConstructorValParameter(Clazz                        clazz,
                                             KotlinClassKindMetadata      kotlinClassKindMetadata,
                                             KotlinConstructorMetadata    kotlinConstructorMetadata,
                                             KotlinValueParameterMetadata kotlinValueParameterMetadata)
    {
        new AssertUtil("Constructor value parameter", reporter, programClassPool, libraryClassPool)
            .reportIfNullReference("type", kotlinValueParameterMetadata.type);
    }

    @Override
    public void visitFunctionValParameter(Clazz                        clazz,
                                          KotlinMetadata               kotlinMetadata,
                                          KotlinFunctionMetadata       kotlinFunctionMetadata,
                                          KotlinValueParameterMetadata kotlinValueParameterMetadata)
    {
        new AssertUtil("Function value parameter", reporter, programClassPool, libraryClassPool)
            .reportIfNullReference("type", kotlinValueParameterMetadata.type);
    }

    @Override
    public void visitPropertyValParameter(Clazz                              clazz,
                                          KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                          KotlinPropertyMetadata             kotlinPropertyMetadata,
                                          KotlinValueParameterMetadata       kotlinValueParameterMetadata)
    {
        new AssertUtil("Property value parameter", reporter, programClassPool, libraryClassPool)
            .reportIfNullReference("type", kotlinValueParameterMetadata.type);
    }
}
