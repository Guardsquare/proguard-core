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
package proguard.classfile.kotlin.visitor;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinClassKindMetadata;
import proguard.classfile.kotlin.KotlinMetadata;

import static proguard.classfile.TypeConstants.INNER_CLASS_SEPARATOR;

/**
 * This {@link KotlinMetadataVisitor} travels to the anonymous object origin class and
 * delegates to the given {@link KotlinMetadataVisitor}.
 *
 * @author James Hamilton
 */
public class KotlinClassToAnonymousObjectOriginClassVisitor
implements   KotlinMetadataVisitor
{
    private final ClassPool             classPool;
    private final KotlinMetadataVisitor kotlinMetadataVisitor;

    public KotlinClassToAnonymousObjectOriginClassVisitor(ClassPool             classPool,
                                                          KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.classPool             = classPool;
        this.kotlinMetadataVisitor = kotlinMetadataVisitor;
    }

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) { }

    @Override
    public void visitKotlinClassMetadata(Clazz clazz, KotlinClassKindMetadata kotlinClassKindMetadata)
    {
        if (kotlinClassKindMetadata.anonymousObjectOriginName == null)
        {
            int endIndex = clazz.getName().indexOf(INNER_CLASS_SEPARATOR);
            if (endIndex == -1)
            {
                endIndex = clazz.getName().length();
            }
            String className = clazz.getName().substring(0, endIndex);
            Clazz _clazz = this.classPool.getClass(className);
            if (_clazz != null)
            {
                _clazz.kotlinMetadataAccept(this.kotlinMetadataVisitor);
            }
        }
        else
        {
            if (this.classPool.getClass(kotlinClassKindMetadata.anonymousObjectOriginName) != null)
            {
                this.classPool.getClass(kotlinClassKindMetadata.anonymousObjectOriginName).kotlinMetadataAccept(this);
            }
            else
            {
                int endIndex = kotlinClassKindMetadata.anonymousObjectOriginName.indexOf(INNER_CLASS_SEPARATOR);
                if (endIndex == -1)
                {
                    endIndex = kotlinClassKindMetadata.anonymousObjectOriginName.length();
                }
                String aon = kotlinClassKindMetadata.anonymousObjectOriginName.substring(0, endIndex);
                Clazz aoc = this.classPool.getClass(aon);
                if (aoc != null)
                {
                    aoc.kotlinMetadataAccept(this.kotlinMetadataVisitor);
                }
            }
        }
    }
}
