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
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.util.kotlin.asserter.Reporter;

/**
 * Implementations of this class represent a conceptual constraint on KotlinMetadata.
 *
 * A KotlinMetadataConstraint is checked in the context of ClassPools and a specific KotlinMetadata
 * instance, and should report its findings to the passed Reporter.
 */
public interface KotlinAsserterConstraint
{
    void check(Reporter       reporter,
               ClassPool      programClassPool,
               ClassPool      libraryClassPool,
               Clazz          clazz,
               KotlinMetadata kotlinMetadata);

    void check(Reporter reporter, KotlinModule kotlinModule);
}
