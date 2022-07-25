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
import proguard.classfile.kotlin.visitor.AllTypeVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinTypeFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor
import java.util.function.Predicate

class KotlinTypeFlagsTest : FreeSpec({
    val clazz = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            @Suppress("UNUSED_PARAMETER")
            fun foo(bar: suspend () -> Unit, string: String): Int? = 42
            """
        )
    ).programClassPool.getClass("TestKt")

    "Given a non-nullable type" - {
        "Then the flags should be initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("kotlin/String", typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe false
                        it.flags.isSuspend shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("kotlin/String", typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe false
                        it.flags.isSuspend shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }

    "Given a nullable type" - {
        "Then the flags should be initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("kotlin/Int", typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe true
                        it.flags.isSuspend shouldBe false

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("kotlin/Int", typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe true

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }

    "Given a suspend type" - {
        "Then the flags should be initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("kotlin/Function1", typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe false
                        it.flags.isSuspend shouldBe true

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("kotlin/Function1", typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe false
                        it.flags.isSuspend shouldBe true

                        it.flags.common.hasAnnotations shouldBe false
                    }
                )
            }
        }
    }
})

private fun createVisitor(className: String, typeVisitor: KotlinTypeVisitor): KotlinMetadataVisitor =
    AllTypeVisitor(
        KotlinTypeFilter(
            Predicate { it.className == className },
            typeVisitor
        )
    )
