/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package testutils

import proguard.classfile.Clazz
import proguard.classfile.io.kotlin.KotlinMetadataWriter
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.MultiKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MultiClassVisitor

/**
 * This {@link ClassVisitor} delegates to the given {@link KotlinMetadataVistor}s but only after, for each class:
 *
 * - writing the referencedKotlinMetadata to the clazz's `kotlin.Metadata` annotation
 * - re-initializing the Kotlin metadata from the `kotlin.Metadata` annotation
 *
 * This therefore, ensures that the Kotlin metadata goes through the complete initialize -> write -> initialize cycle.
 */
class ReWritingMetadataVisitor(private vararg val visitors: KotlinMetadataVisitor) : ClassVisitor {
    override fun visitAnyClass(clazz: Clazz?) {

        clazz?.accept(
            MultiClassVisitor(
                KotlinMetadataWriter { _, message -> println(message) },
                KotlinMetadataInitializer { _, message -> println(message) },
                ReferencedKotlinMetadataVisitor(
                    MultiKotlinMetadataVisitor(*visitors)
                )
            )
        )
    }
}
