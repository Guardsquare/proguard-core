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


package proguard.classfile.kotlin;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

/**
 * A Kotlin metadata type that corresponds to metadata that
 * could not be parsed correctly.
 *
 * @author James Hamilton
 */
public class UnsupportedKotlinMetadata extends KotlinMetadata
{

    public UnsupportedKotlinMetadata(int k, int[] mv, int xi, String xs, String pn)
    {
        super(k, mv, xi, xs, pn);
    }


    @Override
    public void accept(Clazz clazz, KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        kotlinMetadataVisitor.visitUnsupportedKotlinMetadata(clazz, this);
    }
}
