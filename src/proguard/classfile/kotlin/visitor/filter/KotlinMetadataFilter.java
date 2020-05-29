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
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

import java.util.function.Predicate;

/**
 * This {@link KotlinMetadataVisitor} delegates its visits to one of two given visitors,
 * depending on whether the given predicate succeeds.
 *
 * @author James Hamilton
 */
public class KotlinMetadataFilter
implements   KotlinMetadataVisitor
{
    private final Predicate<KotlinMetadata> predicate;
    private final KotlinMetadataVisitor     acceptedVisitor;
    private final KotlinMetadataVisitor     rejectedVisitor;

    public KotlinMetadataFilter(KotlinMetadataVisitor acceptedVisitor,
                                KotlinMetadataVisitor rejectedVisitor)
    {
        this(__ -> true, acceptedVisitor, rejectedVisitor);
    }

    public KotlinMetadataFilter(Predicate<KotlinMetadata> predicate,
                                KotlinMetadataVisitor     acceptedVisitor)
    {
        this(predicate, acceptedVisitor, null);
    }

    public KotlinMetadataFilter(Predicate<KotlinMetadata> predicate,
                                KotlinMetadataVisitor     acceptedVisitor,
                                KotlinMetadataVisitor     rejectedVisitor)
    {
        this.predicate       = predicate;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }


    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        KotlinMetadataVisitor visitor = this.getDelegateVisitor(kotlinMetadata);
        if (visitor != null)
        {
            kotlinMetadata.accept(clazz, visitor);
        }
    }

    // Helper methods.

    private KotlinMetadataVisitor getDelegateVisitor(KotlinMetadata kotlinMetadata)
    {
        return this.predicate.test(kotlinMetadata) ? this.acceptedVisitor : this.rejectedVisitor;
    }
}
