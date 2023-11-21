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
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinTypeParameterFilter
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor
import java.util.function.Predicate

class KotlinTypeParameterFlagsTest : FreeSpec({
    val clazz = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            fun <T> foo(): Int = 42
            inline fun <reified ReifiedT> bar(): Int = 42
            """
        )
    ).programClassPool.getClass("TestKt")

    "Given a non-reified type parameter" - {

        "Then the flags should be initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("T", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe false
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("T", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe false
                    }
                )
            }
        }
    }

    "Given a reified type parameter" - {

        "Then the flags should be initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor("ReifiedT", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe true
                    }
                )
            }
        }

        "Then the flags should be written and re-initialized correctly" {
            val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor("ReifiedT", typeParameterVisitor)))

            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    clazz,
                    withArg {
                        it.flags.isReified shouldBe true
                    }
                )
            }
        }
    }

    "Given a type parameter with annotation" - {
        val clazz = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                @Target(AnnotationTarget.TYPE_PARAMETER)
                annotation class Ann
                inline fun <@Ann T> bar(): Int = 42
                """
            )
        ).programClassPool.getClass("TestKt")

        "Then the flags should be initialized correctly" {
            clazz.accept(
                ReferencedKotlinMetadataVisitor(
                    AllTypeParameterVisitor(
                        KotlinTypeParameterFilter(
                            {
                                true
                            },
                            { _, kotlinTypeMetadata ->
                                kotlinTypeMetadata.flags.isReified shouldBe false
                            }
                        )
                    )
                )
            )
        }

        "Then the flags should be written and re-initialized correctly" {
            clazz.accept(
                ReWritingMetadataVisitor(
                    AllTypeParameterVisitor(
                        KotlinTypeParameterFilter(
                            {
                                true
                            },
                            { _, kotlinTypeMetadata ->
                                kotlinTypeMetadata.flags.isReified shouldBe false
                            }
                        )
                    )
                )
            )
        }
    }
})

private fun createVisitor(typeName: String, typeVisitor: KotlinTypeParameterVisitor): KotlinMetadataVisitor =
    AllTypeParameterVisitor(
        KotlinTypeParameterFilter(
            Predicate { it.name == typeName },
            typeVisitor
        )
    )
