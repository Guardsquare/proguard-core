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

package proguard.classfile.kotlin.flags

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.visitor.AllPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor

class KotlinPropertyAccessorFlagsTest : FreeSpec({

    "Given a property with default getter and setter" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", "val foo: Int = 1".trimIndent())
        ).programClassPool.getClass("TestKt")

        "Then the property accessor flags should be set accordingly" {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterFlags.isDefault shouldBe true }
                        withClue("isInline") { it.getterFlags.isInline shouldBe false }
                        withClue("isExternal") { it.getterFlags.isExternal shouldBe false }
                        // setterFlags shoquld be set correctly
                        withClue("isExternal") { it.setterFlags.isDefault shouldBe true }
                        withClue("isExternal") { it.setterFlags.isInline shouldBe false }
                        withClue("isExternal") { it.setterFlags.isExternal shouldBe false }
                    }
                )
            }
        }

        "Then the property accessor flags should be written and re-initialized correctly" {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterFlags.isDefault shouldBe true }
                        withClue("isInline") { it.getterFlags.isInline shouldBe false }
                        withClue("isExternal") { it.getterFlags.isExternal shouldBe false }
                        // setterFlags should be set correctly
                        withClue("isExternal") { it.setterFlags.isDefault shouldBe true }
                        withClue("isExternal") { it.setterFlags.isInline shouldBe false }
                        withClue("isExternal") { it.setterFlags.isExternal shouldBe false }
                    }
                )
            }
        }
    }

    "Given a property with a non-default, inlined getter" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", "val foo: Int inline get() = 1".trimIndent())
        ).programClassPool.getClass("TestKt")

        "Then the property accessor flags should be set accordingly" {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterFlags.isDefault shouldBe false }
                        withClue("isInline") { it.getterFlags.isInline shouldBe true }
                        withClue("isExternal") { it.getterFlags.isExternal shouldBe false }
                        // setterFlags should be set correctly
                        withClue("isExternal") { it.setterFlags.isDefault shouldBe true }
                        withClue("isExternal") { it.setterFlags.isInline shouldBe false }
                        withClue("isExternal") { it.setterFlags.isExternal shouldBe false }
                    }
                )
            }
        }

        "Then the property accessor flags should written and re-initialized correctly" {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterFlags.isDefault shouldBe false }
                        withClue("isInline") { it.getterFlags.isInline shouldBe true }
                        withClue("isExternal") { it.getterFlags.isExternal shouldBe false }
                        // setterFlags should be set correctly
                        withClue("isExternal") { it.setterFlags.isDefault shouldBe true }
                        withClue("isExternal") { it.setterFlags.isInline shouldBe false }
                        withClue("isExternal") { it.setterFlags.isExternal shouldBe false }
                    }
                )
            }
        }
    }

    // TODO(T5486) add test with non-default external getter
})
