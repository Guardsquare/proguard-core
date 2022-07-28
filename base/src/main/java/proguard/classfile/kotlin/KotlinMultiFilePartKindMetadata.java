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
package proguard.classfile.kotlin;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

import static proguard.classfile.kotlin.KotlinConstants.METADATA_KIND_MULTI_FILE_CLASS_PART;

public class KotlinMultiFilePartKindMetadata
extends KotlinDeclarationContainerMetadata
{
    public String facadeName;
    public Clazz  referencedFacadeClass;


    public KotlinMultiFilePartKindMetadata(int[]  mv,
                                           int    xi,
                                           String xs,
                                           String pn)
    {
        super(METADATA_KIND_MULTI_FILE_CLASS_PART, mv, xi, xs, pn);

        this.facadeName = xs;
    }


    @Override
    public void accept(Clazz clazz, KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        kotlinMetadataVisitor.visitKotlinMultiFilePartMetadata(clazz, this);
    }


    // Implementations for Object.
    @Override
    public String toString()
    {
        return "Kotlin file part";
    }
}
