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

package proguard.classfile.editor

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import proguard.classfile.util.ClassReferenceInitializer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

/**
 * Regression tests for https://github.com/Guardsquare/proguard-core/issues/174.
 *
 * When the class pool is incomplete -- e.g. a class referenced only from `@Metadata` isn't on the
 * classpath -- [ClassReferenceInitializer] leaves the matching `referenced*` field null.
 * [ClassReferenceFixer] must degrade gracefully in that case rather than throwing a
 * [NullPointerException] while fixing the Kotlin metadata.
 */
class ClassReferenceFixerDanglingReferenceTest : FunSpec({

    test("A dangling Kotlin type reference does not crash the ClassReferenceFixer") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Holder.kt",
                """
                class Referenced

                class Holder {
                    val held: Referenced = Referenced()
                    fun make(): Referenced = Referenced()
                }
                """.trimIndent(),
            ),
        )

        // Simulate an incomplete class pool: remove the referenced class and reinitialize so
        // Holder's Kotlin type metadata is left with null referencedClass fields.
        programClassPool.removeClass("Referenced")
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        shouldNotThrowAny {
            programClassPool.classesAccept(ClassReferenceFixer(false))
        }
    }
})
