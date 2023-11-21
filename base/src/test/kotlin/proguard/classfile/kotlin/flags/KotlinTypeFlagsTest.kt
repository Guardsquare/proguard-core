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
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinTypeFlagsTest : FreeSpec({
    val clazz = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            @Suppress("UNUSED_PARAMETER")
            fun foo(bar: suspend () -> Unit, string: String): Int? = 42
            fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y
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
                    }
                )
            }
        }
    }

    "Given a definitely non-null type" - {
        "Then the flags should be initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor(0, typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isNullable shouldBe false
                        it.flags.isSuspend shouldBe false
                        it.flags.isDefinitelyNonNull shouldBe true
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeVisitor = spyk<KotlinTypeVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor(0, typeVisitor)))

            verify {
                typeVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.flags.isDefinitelyNonNull shouldBe true
                    }
                )
            }
        }
    }

    "Given a type argument with name annotation" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                val namedParam: (test: String) -> Unit = {}
                """
            )
        ).programClassPool.getClass("TestKt")

        "Then the flags should be initialized correctly" {
            clazz.accept(
                ReferencedKotlinMetadataVisitor(
                    AllTypeVisitor(
                        KotlinTypeFilter(
                            {
                                it.className == "kotlin/Function1" && it.typeArguments.size == 2
                            },
                            { _, kotlinTypeMetadata ->
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isNullable shouldBe false
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isSuspend shouldBe false
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isDefinitelyNonNull shouldBe false
                            }
                        )
                    )
                )
            )
        }

        "Then the flags should be written and re-initialized correctly" {
            clazz.accept(
                ReWritingMetadataVisitor(
                    AllTypeVisitor(
                        KotlinTypeFilter(
                            {
                                it.className == "kotlin/Function1" && it.typeArguments.size == 2
                            },
                            { _, kotlinTypeMetadata ->
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isNullable shouldBe false
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isSuspend shouldBe false
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isDefinitelyNonNull shouldBe false
                            }
                        )
                    )
                )
            )
        }
    }

    "Given a nullable type argument" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                val namedParam: (String?) -> Unit = {}
                """
            )
        ).programClassPool.getClass("TestKt")

        "Then the flags should be initialized correctly" {
            clazz.accept(
                ReferencedKotlinMetadataVisitor(
                    AllTypeVisitor(
                        KotlinTypeFilter(
                            {
                                it.className == "kotlin/Function1" && it.typeArguments.size == 2
                            },
                            { _, kotlinTypeMetadata ->
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isNullable shouldBe true
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isSuspend shouldBe false
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isDefinitelyNonNull shouldBe false
                            }
                        )
                    )
                )
            )
        }

        "Then the flags should be written and re-initialized correctly" {
            clazz.accept(
                ReWritingMetadataVisitor(
                    AllTypeVisitor(
                        KotlinTypeFilter(
                            {
                                it.className == "kotlin/Function1" && it.typeArguments.size == 2
                            },
                            { _, kotlinTypeMetadata ->
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isNullable shouldBe true
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isSuspend shouldBe false
                                kotlinTypeMetadata.typeArguments?.get(0)?.flags?.isDefinitelyNonNull shouldBe false
                            }
                        )
                    )
                )
            )
        }
    }
})

private fun createVisitor(className: String, typeVisitor: KotlinTypeVisitor): KotlinMetadataVisitor =
    AllTypeVisitor(
        KotlinTypeFilter(
            { it.className == className },
            typeVisitor
        )
    )

private fun createVisitor(typeParamId: Int, typeVisitor: KotlinTypeVisitor): KotlinMetadataVisitor =
    AllTypeVisitor(
        KotlinTypeFilter(
            { it.typeParamID == typeParamId },
            typeVisitor
        )
    )
