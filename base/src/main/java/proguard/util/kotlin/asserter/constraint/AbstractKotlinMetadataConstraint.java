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

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.util.kotlin.asserter.Reporter;

public abstract class AbstractKotlinMetadataConstraint
implements KotlinAsserterConstraint,
           KotlinMetadataVisitor
{
    protected Reporter  reporter;
    protected ClassPool programClassPool;
    protected ClassPool libraryClassPool;


    @Override
    public void visitAnyKotlinMetadata(Clazz clazz, KotlinMetadata kotlinMetadata) {}

    @Override
    public void check(Reporter       reporter,
                      ClassPool      programClassPool,
                      ClassPool      libraryClassPool,
                      Clazz          clazz,
                      KotlinMetadata metadata)
    {
        this.reporter         = reporter;
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;

        try
        {
            metadata.accept(clazz, this);
        }
        catch (Exception e)
        {
            reporter.report( "Encountered unexpected Exception when checking constraint: " + e.getMessage());
        }
    }

    @Override
    public void check(Reporter reporter, KotlinModule kotlinModule)
    {

    }
}
