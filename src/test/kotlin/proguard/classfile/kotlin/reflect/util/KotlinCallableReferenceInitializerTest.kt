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

package proguard.classfile.kotlin.reflect.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.KotlinFileFacadeKindMetadata
import proguard.classfile.kotlin.KotlinMetadata
import proguard.classfile.kotlin.KotlinSyntheticClassKindMetadata
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import testutils.ClassPoolBuilder
import testutils.JavaSource
import testutils.KotlinSource

class KotlinCallableReferenceInitializerTest : FreeSpec({

    // The initializer (and subsequently, the KotlinCallableReferenceInitializer)
    // is run by the ClassPoolBuilder, so no need to run it in the tests here.

    "Given a function callable reference" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                fun foo() = "bar"
                fun ref() = ::foo
                """.trimIndent()
            )
        )

        val callableRefInfoVisitor = spyk<CallableReferenceInfoVisitor>()
        val ownerVisitor = spyk< KotlinMetadataVisitor>()
        val testVisitor = createVisitor(callableRefInfoVisitor, ownerVisitor)

        programClassPool.classesAccept("TestKt\$ref\$1", testVisitor)

        "Then the callableReferenceInfo should be initialized" {
            verify(exactly = 1) {
                callableRefInfoVisitor.visitFunctionReferenceInfo(
                    withArg {
                        it.name shouldBe "foo"
                        it.signature shouldBe "foo()Ljava/lang/String;"
                        it.owner shouldNotBe null
                    }
                )
            }

            verify(exactly = 1) {
                ownerVisitor.visitKotlinFileFacadeMetadata(
                    programClassPool.getClass("TestKt"),
                    ofType(KotlinFileFacadeKindMetadata::class)
                )
            }
        }
    }

    "Given a property callable reference" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                class Foo {
                    var foo = "bar"
                }

                fun ref() = Foo()::foo
                """.trimIndent()
            )
        )

        val callableRefInfoVisitor = spyk<CallableReferenceInfoVisitor>()
        val ownerVisitor = spyk< KotlinMetadataVisitor>()
        val testVisitor = createVisitor(callableRefInfoVisitor, ownerVisitor)

        programClassPool.classesAccept("TestKt\$ref\$1", testVisitor)

        "Then the callableReferenceInfo should be initialized" {
            verify(exactly = 1) {
                callableRefInfoVisitor.visitPropertyReferenceInfo(
                    withArg {
                        it.name shouldBe "foo"
                        it.signature shouldBe "getFoo()Ljava/lang/String;"
                        it.owner shouldNotBe null
                    }
                )
            }

            verify(exactly = 1) {
                ownerVisitor.visitKotlinClassMetadata(
                    programClassPool.getClass("Foo"),
                    ofType(KotlinClassKindMetadata::class)
                )
            }
        }
    }

    "Given a Java method callable reference" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                val javaClassInstance = Foo()
                fun ref() = javaClassInstance::foo
                """.trimIndent()
            ),
            JavaSource(
                "Foo.java",
                """
                public class Foo {
                    public String foo() { return "bar"; }
                }
                """.trimIndent()
            )
        )

        val callableRefInfoVisitor = spyk<CallableReferenceInfoVisitor>()
        val ownerVisitor = spyk< KotlinMetadataVisitor>()
        val testVisitor = createVisitor(callableRefInfoVisitor, ownerVisitor)

        programClassPool.classesAccept("TestKt\$ref\$1", testVisitor)

        "Then the callableReferenceInfo should be initialized" {
            verify(exactly = 1) {
                callableRefInfoVisitor.visitJavaReferenceInfo(
                    withArg {
                        it.name shouldBe "foo"
                        it.signature shouldBe "foo()Ljava/lang/String;"
                        it.owner shouldBe null
                    }
                )
            }

            verify(exactly = 0) {
                ownerVisitor.visitAnyKotlinMetadata(
                    ofType(Clazz::class),
                    ofType(KotlinMetadata::class)
                )
            }
        }
    }

    "Given a Java field callable reference" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                val javaClassInstance = Foo()
                fun ref() = javaClassInstance::foo
                """.trimIndent()
            ),
            JavaSource(
                "Foo.java",
                """
                public class Foo {
                    public String foo = "bar";
                }
                """.trimIndent()
            )
        )

        val callableRefInfoVisitor = spyk<CallableReferenceInfoVisitor>()
        val ownerVisitor = spyk< KotlinMetadataVisitor>()
        val testVisitor = createVisitor(callableRefInfoVisitor, ownerVisitor)

        programClassPool.classesAccept("TestKt\$ref\$1", testVisitor)

        "Then the callableReferenceInfo should be initialized" {
            verify(exactly = 1) {
                callableRefInfoVisitor.visitJavaReferenceInfo(
                    withArg {
                        it.name shouldBe "foo"
                        it.signature shouldBe "getFoo()Ljava/lang/String;"
                        it.owner shouldBe null
                    }
                )
            }

            verify(exactly = 0) {
                ownerVisitor.visitAnyKotlinMetadata(
                    ofType(Clazz::class),
                    ofType(KotlinMetadata::class)
                )
            }
        }
    }
})

private fun createVisitor(callableRefInfoVisitor: CallableReferenceInfoVisitor, ownerVisitor: KotlinMetadataVisitor) = ReferencedKotlinMetadataVisitor(
    object : KotlinMetadataVisitor {
        override fun visitAnyKotlinMetadata(clazz: Clazz, kotlinMetadata: KotlinMetadata) {}
        override fun visitKotlinSyntheticClassMetadata(
            clazz: Clazz,
            syntheticClassMetadata: KotlinSyntheticClassKindMetadata
        ) {
            syntheticClassMetadata.callableReferenceInfoAccept(callableRefInfoVisitor)
            syntheticClassMetadata.callableReferenceInfoAccept { it.ownerAccept(ownerVisitor) }
        }
    }
)
