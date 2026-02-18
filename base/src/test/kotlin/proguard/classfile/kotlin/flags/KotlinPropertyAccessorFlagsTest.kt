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
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.visitor.AllPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinPropertyAccessorFlagsTest : BehaviorSpec({

    Given("a value property with default getter") {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", "val foo: Int = 1".trimIndent()),
        ).programClassPool.getClass("TestKt")

        Then("the property accessor flags should be set accordingly") {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterMetadata.isDefault shouldBe true }
                        withClue("isInline") { it.getterMetadata.isInline shouldBe false }
                        withClue("isExternal") { it.getterMetadata.isExternal shouldBe false }
                        withClue("annotations") { it.getterMetadata.hasAnnotations shouldBe false }
                        withClue("hasAnnotationsInBytecode") { it.getterMetadata.hasAnnotationsInBytecode shouldBe false }
                        // Value properties do not have setters.
                        it.setterMetadata shouldBe null
                    },
                )
            }
        }

        Then("the property accessor flags should be written and re-initialized correctly") {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterMetadata.isDefault shouldBe true }
                        withClue("isInline") { it.getterMetadata.isInline shouldBe false }
                        withClue("isExternal") { it.getterMetadata.isExternal shouldBe false }
                        withClue("annotations") { it.getterMetadata.hasAnnotations shouldBe false }
                        withClue("hasAnnotationsInBytecode") { it.getterMetadata.hasAnnotationsInBytecode shouldBe false }
                        // Value properties do not have setters.
                        it.setterMetadata shouldBe null
                    },
                )
            }
        }
    }

    Given("a property with a non-default, inlined getter") {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource("Test.kt", "val foo: Int inline get() = 1".trimIndent()),
        ).programClassPool.getClass("TestKt")

        Then("the property accessor flags should be set accordingly") {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterMetadata.isDefault shouldBe false }
                        withClue("isInline") { it.getterMetadata.isInline shouldBe true }
                        withClue("isExternal") { it.getterMetadata.isExternal shouldBe false }
                        withClue("annotations") { it.getterMetadata.hasAnnotations shouldBe false }
                        withClue("hasAnnotationsInBytecode") { it.getterMetadata.hasAnnotationsInBytecode shouldBe false }
                        // Value properties do not have setters.
                        it.setterMetadata shouldBe null
                    },
                )
            }
        }

        Then("the property accessor flags should written and re-initialized correctly") {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("isDefault") { it.getterMetadata.isDefault shouldBe false }
                        withClue("isInline") { it.getterMetadata.isInline shouldBe true }
                        withClue("isExternal") { it.getterMetadata.isExternal shouldBe false }
                        withClue("annotations") { it.getterMetadata.hasAnnotations shouldBe false }
                        withClue("hasAnnotationsInBytecode") { it.getterMetadata.hasAnnotationsInBytecode shouldBe false }
                        // Value properties do not have setters.
                        it.setterMetadata shouldBe null
                    },
                )
            }
        }
    }

    Given("a value property with annotated getter and setter") {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
            annotation class Ann
            @get:Ann
            @set:Ann
            var foo: Int = 1
                """.trimIndent().trimIndent(),
            ),
        ).programClassPool.getClass("TestKt")

        Then("the property accessor flags should be set accordingly") {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("getter isDefault") { it.getterMetadata.isDefault shouldBe true }
                        withClue("getter isInline") { it.getterMetadata.isInline shouldBe false }
                        withClue("getter isExternal") { it.getterMetadata.isExternal shouldBe false }
                        withClue("getter annotations") { it.getterMetadata.hasAnnotations shouldBe true }
                        withClue("getter hasAnnotationsInBytecode") { it.getterMetadata.hasAnnotationsInBytecode shouldBe true }
                        // setterFlags should be set correctly
                        withClue("setter isDefault") { it.setterMetadata?.isDefault shouldBe true }
                        withClue("setter isInline") { it.setterMetadata?.isInline shouldBe false }
                        withClue("setter isExternal") { it.setterMetadata?.isExternal shouldBe false }
                        withClue("setter annotations") { it.setterMetadata?.hasAnnotations shouldBe true }
                        withClue("setter hasAnnotationsInBytecode") { it.setterMetadata?.hasAnnotationsInBytecode shouldBe true }
                    },
                )
            }
        }

        Then("the property accessor flags should be written and re-initialized correctly") {
            val propertyVisitor = spyk<KotlinPropertyVisitor>()
            clazz.accept(ReWritingMetadataVisitor(AllPropertyVisitor(propertyVisitor)))

            verify {
                propertyVisitor.visitProperty(
                    clazz,
                    ofType(KotlinDeclarationContainerMetadata::class),
                    withArg {
                        // getterFlags should be set correctly
                        withClue("getter isDefault") { it.getterMetadata.isDefault shouldBe true }
                        withClue("getter isInline") { it.getterMetadata.isInline shouldBe false }
                        withClue("getter isExternal") { it.getterMetadata.isExternal shouldBe false }
                        withClue("getter annotations") { it.getterMetadata.hasAnnotations shouldBe true }
                        withClue("getter hasAnnotationsInBytecode") { it.getterMetadata.hasAnnotationsInBytecode shouldBe true }
                        // setterFlags should be set correctly
                        withClue("setter isDefault") { it.setterMetadata?.isDefault shouldBe true }
                        withClue("setter isInline") { it.setterMetadata?.isInline shouldBe false }
                        withClue("setter isExternal") { it.setterMetadata?.isExternal shouldBe false }
                        withClue("setter annotations") { it.setterMetadata?.hasAnnotations shouldBe true }
                        withClue("setter hasAnnotationsInBytecode") { it.setterMetadata?.hasAnnotationsInBytecode shouldBe true }
                    },
                )
            }
        }
    }

    // TODO(T5486) add test with non-default external getter
})
