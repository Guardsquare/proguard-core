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
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

import java.util.function.Predicate;

/**
 * Filter KotlinDeclarationContainers, based on the given predicate.
 *
 * @author James Hamilton
 */
public class KotlinDeclarationContainerFilter
implements   KotlinMetadataVisitor
{
    private final Predicate<KotlinDeclarationContainerMetadata> predicate;
    private final KotlinMetadataVisitor acceptedVisitor;
    private final KotlinMetadataVisitor rejectedVisitor;


    public KotlinDeclarationContainerFilter(KotlinMetadataVisitor acceptedVisitor)
    {
        this(__ -> true, acceptedVisitor, null);
    }

    public KotlinDeclarationContainerFilter(Predicate<KotlinDeclarationContainerMetadata> predicate,
                                            KotlinMetadataVisitor                         acceptedVisitor)
    {
        this(predicate, acceptedVisitor, null);
    }

    public KotlinDeclarationContainerFilter(Predicate<KotlinDeclarationContainerMetadata> predicate,
                                            KotlinMetadataVisitor                         acceptedVisitor,
                                            KotlinMetadataVisitor                         rejectedVisitor)
    {
        this.predicate       = predicate;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void visitKotlinDeclarationContainerMetadata(Clazz                              clazz,
                                                        KotlinDeclarationContainerMetadata kotlinDeclarationContainerMetadata)
    {
        KotlinMetadataVisitor visitor = this.predicate.test(kotlinDeclarationContainerMetadata) ? acceptedVisitor : rejectedVisitor;
        if (visitor != null)
        {
            visitor.visitKotlinDeclarationContainerMetadata(clazz, kotlinDeclarationContainerMetadata);
        }
    }
}
