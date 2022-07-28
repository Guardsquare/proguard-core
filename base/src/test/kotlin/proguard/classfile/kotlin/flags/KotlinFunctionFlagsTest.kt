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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinMetadata
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor
import java.util.function.Predicate

class KotlinFunctionFlagsTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            class Foo {
                fun declared() = "foo"
                infix fun infixFun(param: String) = param
                operator fun plus(param: String) = param
            }

            @Suppress("NOTHING_TO_INLINE")
            inline fun inlineFun() = "foo"
            suspend fun suspendFun() = "foo"
            tailrec fun tailrecFun(): String = tailrecFun()
            """.trimIndent()
        )
    )

    "Given a normal function in a class" - {
        val clazz = programClassPool.getClass("Foo")

        "Then the isDeclared flag should be set when initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcVisitor, "declared")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isDeclaration shouldBe true
                    }
                )
            }
        }

        "Then the isDeclared flag should be set when written and re-initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReWritingMetadataVisitor(createVisitor(funcVisitor, "declared")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isDeclaration shouldBe true
                    }
                )
            }
        }
    }

    "Given an inline function" - {
        val clazz = programClassPool.getClass("TestKt")

        "Then the isInline flag should be set when initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcVisitor, "inlineFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isInline shouldBe true
                    }
                )
            }
        }

        "Then the isInline flag should be set when written and re-initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReWritingMetadataVisitor(createVisitor(funcVisitor, "inlineFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isInline shouldBe true
                    }
                )
            }
        }
    }

    "Given an infix function" - {
        val clazz = programClassPool.getClass("Foo")

        "Then the isInfix flag should be set when initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcVisitor, "infixFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isInfix shouldBe true
                    }
                )
            }
        }

        "Then the isInfix flag should be set when written and re-initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReWritingMetadataVisitor(createVisitor(funcVisitor, "infixFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isInfix shouldBe true
                    }
                )
            }
        }
    }

    "Given an operator function" - {
        val clazz = programClassPool.getClass("Foo")

        "Then the isOperator flag should be set when initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcVisitor, "plus")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isOperator shouldBe true
                    }
                )
            }
        }

        "Then the isOperator flag should be set when written and re-initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReWritingMetadataVisitor(createVisitor(funcVisitor, "plus")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isOperator shouldBe true
                    }
                )
            }
        }
    }

    "Given an suspend function" - {
        val clazz = programClassPool.getClass("TestKt")

        "Then the isSuspend flag should be set when initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcVisitor, "suspendFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isSuspend shouldBe true
                    }
                )
            }
        }

        "Then the isSuspend flag should be set when written and re-initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReWritingMetadataVisitor(createVisitor(funcVisitor, "suspendFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isSuspend shouldBe true
                    }
                )
            }
        }
    }

    "Given a tailrec function" - {
        val clazz = programClassPool.getClass("TestKt")

        "Then the isTailrec flag should be set when initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(funcVisitor, "tailrecFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isTailrec shouldBe true
                    }
                )
            }
        }

        "Then the isTailrec flag should be set when written and re-initialized" {
            val funcVisitor = spyk<KotlinFunctionVisitor>()
            clazz.accept(ReWritingMetadataVisitor(createVisitor(funcVisitor, "tailrecFun")))

            verify(exactly = 1) {
                funcVisitor.visitAnyFunction(
                    clazz,
                    ofType(KotlinMetadata::class),
                    withArg {
                        it.flags.isTailrec shouldBe true
                    }
                )
            }
        }
    }

    // TODO isFakeOverride
    // TODO isSynthesized
    // TODO isExternal
    // TODO isExpect
})

private fun createVisitor(funcVisitor: KotlinFunctionVisitor, name: String) = AllFunctionVisitor(
    KotlinFunctionFilter(
        Predicate { it.name == name },
        funcVisitor
    )
)
