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

package proguard.classfile.util.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinDeclarationContainerFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class KotlinMetadataInitializerTest : FreeSpec({

    "Given a file facade containing 1 class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
            val property: String = "FOO"

            class Foo
                """.trimIndent()
            )
        )

        "Then the file facade ownerClassName should be correct" {
            val declarationContainerVisitor = spyk<KotlinMetadataVisitor>()

            programClassPool.classesAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(
                    KotlinDeclarationContainerFilter(declarationContainerVisitor)
                )
            )

            verify(exactly = 1) {
                declarationContainerVisitor.visitKotlinDeclarationContainerMetadata(
                    programClassPool.getClass("TestKt"),
                    withArg {
                        it.ownerClassName = "TestKt"
                    }
                )
            }
        }

        "Then the class ownerClassName should be correct" {
            val declarationContainerVisitor = spyk<KotlinMetadataVisitor>()

            programClassPool.classesAccept(
                "Foo",
                ReferencedKotlinMetadataVisitor(
                    KotlinDeclarationContainerFilter(declarationContainerVisitor)
                )
            )

            verify(exactly = 1) {
                declarationContainerVisitor.visitKotlinDeclarationContainerMetadata(
                    programClassPool.getClass("Foo"),
                    withArg {
                        it.ownerClassName = "Foo"
                    }
                )
            }
        }
    }
})
