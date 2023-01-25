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
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinPropertyMetadata;
import proguard.classfile.kotlin.visitor.AllPropertyVisitor;
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

/**
 * This class checks the assumption: All properties need a JVM signature for their getter
 */
public class PropertyIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinPropertyVisitor
{

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new AllPropertyVisitor(this));
    }

    // Implementations for KotlinPropertyVisitor.

    @Override
    public void visitAnyProperty(Clazz                              clazz,
                                 KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                 KotlinPropertyMetadata             kotlinPropertyMetadata)
    {
        AssertUtil util = new AssertUtil("Property " + kotlinPropertyMetadata.name, reporter, programClassPool, libraryClassPool);

        util.reportIfNull("backingFieldSignature, getterSignature or setterSignature",
                          kotlinPropertyMetadata.backingFieldSignature,
                          kotlinPropertyMetadata.getterSignature,
                          kotlinPropertyMetadata.setterSignature);

        if (kotlinPropertyMetadata.backingFieldSignature  != null)
        {
            util.reportIfNullReference("backing field class",
                                       kotlinPropertyMetadata.referencedBackingFieldClass);
            util.reportIfClassDangling("backing field class",
                                       kotlinPropertyMetadata.referencedBackingFieldClass);
            util.reportIfNullReference("backing field",
                                       kotlinPropertyMetadata.referencedBackingField);
            util.reportIfFieldDangling("backing field",
                                       kotlinPropertyMetadata.referencedBackingFieldClass,
                                       kotlinPropertyMetadata.referencedBackingField);
        }

        if (kotlinPropertyMetadata.getterSignature != null)
        {
            util.reportIfNullReference("getter", kotlinPropertyMetadata.referencedGetterMethod);
        }

        if (kotlinPropertyMetadata.setterSignature != null)
        {
            util.reportIfNullReference("setter", kotlinPropertyMetadata.referencedSetterMethod);
        }

        if (kotlinPropertyMetadata.syntheticMethodForAnnotations != null)
        {
            util.reportIfNullReference("synthetic annotations method class",
                                       kotlinPropertyMetadata.referencedSyntheticMethodClass);
            util.reportIfClassDangling("synthetic annotations method class",
                                       kotlinPropertyMetadata.referencedSyntheticMethodClass);
            util.reportIfNullReference("synthetic annotations method",
                                       kotlinPropertyMetadata.referencedSyntheticMethodForAnnotations);
            util.reportIfMethodDangling("synthetic annotations method",
                                        kotlinPropertyMetadata.referencedSyntheticMethodClass,
                                        kotlinPropertyMetadata.referencedSyntheticMethodForAnnotations);
        }

        if (kotlinPropertyMetadata.syntheticMethodForDelegate != null)
        {
            util.reportIfNullReference("synthetic delegate method class",
                                       kotlinPropertyMetadata.referencedSyntheticMethodForDelegateClass);
            util.reportIfClassDangling("synthetic delegate method class",
                                       kotlinPropertyMetadata.referencedSyntheticMethodForDelegateClass);
            util.reportIfNullReference("synthetic delegate method",
                                       kotlinPropertyMetadata.referencedSyntheticMethodForDelegateMethod);
            util.reportIfMethodDangling("synthetic delegate method",
                                        kotlinPropertyMetadata.referencedSyntheticMethodForDelegateClass,
                                        kotlinPropertyMetadata.referencedSyntheticMethodForDelegateMethod);

        }
    }
}
