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
