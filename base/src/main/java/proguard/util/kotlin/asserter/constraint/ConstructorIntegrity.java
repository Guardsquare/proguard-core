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
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.AllConstructorVisitor;
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

public class ConstructorIntegrity
extends      AbstractKotlinMetadataConstraint
implements   KotlinConstructorVisitor
{

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new AllConstructorVisitor(this));
    }

    @Override
    public void visitConstructor(Clazz                     clazz,
                                 KotlinClassKindMetadata   kotlinClassKindMetadata,
                                 KotlinConstructorMetadata kotlinConstructorMetadata)
    {
        if (!kotlinClassKindMetadata.flags.isAnnotationClass)
        {
            AssertUtil util = new AssertUtil("Constructor", reporter, programClassPool, libraryClassPool);
            util.reportIfNullReference("constructor referencedMethod", kotlinConstructorMetadata.referencedMethod);
            util.reportIfMethodDangling("constructor referencedMethod",
                                        clazz, kotlinConstructorMetadata.referencedMethod);
        }
    }
}
