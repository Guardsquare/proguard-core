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
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.KotlinTypeMetadata;
import proguard.classfile.kotlin.visitor.AllTypeVisitor;
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor;
import proguard.util.kotlin.asserter.AssertUtil;

/**
 * This class checks the assumption: All properties need a JVM signature for their getter
 */
public class TypeIntegrity
extends    AbstractKotlinMetadataConstraint
implements KotlinTypeVisitor
{

    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata)
    {
        kotlinMetadata.accept(clazz, new AllTypeVisitor(this));
    }


    // Implementations for KotlinPropertyVisitor.
    @Override
    public void visitAnyType(Clazz              clazz,
                             KotlinTypeMetadata type)
    {
        AssertUtil util = new AssertUtil("Type", reporter, programClassPool, libraryClassPool);

        if (type.className != null)
        {
            util.reportIfNullReference("class \"" + type.className + "\"", type.referencedClass);
            util.reportIfClassDangling("class \"" + type.className + "\"", type.referencedClass);

            if (type.aliasName != null)
            {
                reporter.report("Type cannot have both className (" + type.className + ") and aliasName (" + type.aliasName + ")");
            }

            if (type.typeParamID >= 0)
            {
                reporter.report("Type cannot have both className (" + type.className + ") and typeParamID (" + type.typeParamID + ")");
            }
        }

        if (type.aliasName != null)
        {
            util.reportIfNullReference("type alias \"" + type.aliasName + "\"", type.referencedTypeAlias);

            if (type.className != null)
            {
                reporter.report("Type cannot have both className (" + type.className + ") and aliasName (" + type.aliasName + ")");
            }

            if (type.typeParamID >= 0)
            {
                reporter.report("Type cannot have both aliasName (" + type.aliasName + ") and typeParamID (" + type.typeParamID + ")");
            }
        }
    }
}
