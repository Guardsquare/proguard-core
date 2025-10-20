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

package proguard.classfile.kotlin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.MethodSignature
import proguard.classfile.io.kotlin.KotlinMetadataWriter.HIGHEST_ALLOWED_TO_WRITE
import proguard.classfile.io.kotlin.KotlinMetadataWriter.LATEST_STABLE_SUPPORTED
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.AllTypeVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

/**
 * Tests that the KotlinMetadataWriter correctly writes metadata to the
 * kotlin.Metadata annotation attached to classes.
 *
 * The tests here should use `ReWritingMetadataVisitor`: this wrapper
 * writes the current Kotlin metadata to the annotation, thus overwriting
 * the current metadata. It then re-initializes the metadata using `KotlinMetadataInitializer`.
 *
 * If the writer correctly wrote the metadata then the initializer should be able to
 * re-generate the model correctly.
 */
class KotlinMetadataWriterTest : BehaviorSpec({

    Given("a file facade") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                fun foo() {
                    println("foo")
                }

                class Foo

                typealias fooAlias = String

                val myFoo = "bar"
                """.trimIndent(),
            ),
        )

        val metadataVisitor = spyk<KotlinMetadataVisitor>()
        val functionVisitor = spyk<KotlinFunctionVisitor>()

        programClassPool.classesAccept(
            ReWritingMetadataVisitor(
                metadataVisitor,
                AllFunctionVisitor(functionVisitor),
            ),
        )

        Then("the ownerClassName shouldBe correct") {
            verify(exactly = 1) {
                metadataVisitor.visitKotlinFileFacadeMetadata(
                    programClassPool.getClass("TestKt"),
                    withArg {
                        it.ownerClassName shouldBe "TestKt"
                    },
                )
            }
        }

        Then("there should be 1 function") {
            verify(exactly = 1) {
                functionVisitor.visitFunction(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFileFacadeKindMetadata>(),
                    withArg {
                        it.name shouldBe "foo"
                        it.jvmSignature shouldBe MethodSignature(null, "foo", "()V")
                    },
                )
            }
        }
    }

    Given("an inline class") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                @JvmInline
                value class Password(private val s : String)
                """.trimIndent(),
            ),
        )

        val clazz = programClassPool.getClass("Password")
        val kotlinTypeVisitor = spyk<KotlinTypeVisitor>()

        clazz.accept(
            ReWritingMetadataVisitor(
                AllTypeVisitor(kotlinTypeVisitor),
            ),
        )

        Then("the type and name should be correct") {
            verify {
                kotlinTypeVisitor.visitInlineClassUnderlyingPropertyType(
                    clazz,
                    withArg {
                        it.underlyingPropertyName shouldBe "s"
                        it.underlyingPropertyType.className shouldBe "kotlin/String"
                    },
                    ofType<KotlinTypeMetadata>(),
                )
            }
        }
    }

    Given("a Kotlin class with a incompatible metadata version") {
        val unsupportedVersion = KotlinMetadataVersion(1, 3, 0)

        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestCompatibleMetadata.java",
                """
                        @kotlin.Metadata(
                            d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                            d2 = {"LTestCompatibleMetadata;", "", "()V"},
                            k = 1,
                            mv = {${unsupportedVersion.major}, ${unsupportedVersion.minor}, ${unsupportedVersion.patch}}
                        )
                        public class TestCompatibleMetadata { }
                """.trimIndent(),
            ),
        )
        programClassPool.classesAccept(KotlinMetadataInitializer { _, _ -> })
        val clazz = programClassPool.getClass("TestCompatibleMetadata")

        Then("the compatible version from the metadata library should be written") {
            val visitor = spyk<KotlinMetadataVisitor>()

            clazz.accept(ReWritingMetadataVisitor(visitor))

            verify {
                visitor.visitKotlinClassMetadata(
                    clazz,
                    withArg {
                        it.mv shouldBe LATEST_STABLE_SUPPORTED.toArray()
                    },
                )
            }
        }
    }

    Given("a Kotlin class with a compatible metadata version") {
        val maxVersion = HIGHEST_ALLOWED_TO_WRITE

        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestCompatibleMetadata.java",
                """
                        @kotlin.Metadata(
                            d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                            d2 = {"LTestCompatibleMetadata;", "", "()V"},
                            k = 1,
                            mv = {${maxVersion.major}, ${maxVersion.minor}, ${maxVersion.patch}}
                        )
                        public class TestCompatibleMetadata { }
                """.trimIndent(),
            ),
        )
        programClassPool.classesAccept(KotlinMetadataInitializer { _, _ -> })
        val clazz = programClassPool.getClass("TestCompatibleMetadata")

        Then("the compatible version from the metadata library should be written") {
            val visitor = spyk<KotlinMetadataVisitor>()

            clazz.accept(ReWritingMetadataVisitor(visitor))

            verify {
                visitor.visitKotlinClassMetadata(
                    clazz,
                    withArg {
                        it.mv shouldBe HIGHEST_ALLOWED_TO_WRITE.toArray()
                    },
                )
            }
        }
    }
})
