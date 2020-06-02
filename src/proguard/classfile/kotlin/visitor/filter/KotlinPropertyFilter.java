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
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor;

import java.util.function.Predicate;

/**
 * Delegate to another {@link KotlinPropertyVisitor} if the predicate returns true.
 */
public class KotlinPropertyFilter
implements   KotlinPropertyVisitor
{
    private final KotlinPropertyVisitor             acceptedPropertyVisitor;
    private final KotlinPropertyVisitor             rejectedPropertyVisitor;
    private final Predicate<KotlinPropertyMetadata> predicate;


    public KotlinPropertyFilter(Predicate<KotlinPropertyMetadata> predicate,
                                KotlinPropertyVisitor             acceptedPropertyVisitor,
                                KotlinPropertyVisitor             rejectedPropertyVisitor)
    {
        this.acceptedPropertyVisitor = acceptedPropertyVisitor;
        this.rejectedPropertyVisitor = rejectedPropertyVisitor;
        this.predicate               = predicate;
    }

    public KotlinPropertyFilter(Predicate<KotlinPropertyMetadata> predicate,
                                KotlinPropertyVisitor             acceptedPropertyVisitor)
    {
        this(predicate, acceptedPropertyVisitor, null);
    }

    // Implementations for KotlinPropertyVisitor.

    @Override
    public void visitAnyProperty(Clazz                              clazz,
                                 KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata,
                                 KotlinPropertyMetadata             kotlinPropertyMetadata)
    {
        KotlinPropertyVisitor visitor = this.predicate.test(kotlinPropertyMetadata) ? acceptedPropertyVisitor : rejectedPropertyVisitor;
        if (visitor != null)
        {
            visitor.visitAnyProperty(clazz, kotlinDeclarationContainerMetadata, kotlinPropertyMetadata);
        }
    }
}
