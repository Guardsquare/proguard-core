/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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

package proguard.classfile.kotlin.visitor.filter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinTypeAliasMetadata
import proguard.classfile.kotlin.KotlinTypeMetadata
import proguard.classfile.kotlin.KotlinTypeParameterMetadata
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import java.util.function.Predicate

class KotlinAnnotationFilterTest : FreeSpec({

    "Given a Kotlin file facade with annotated entities" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                @Target(AnnotationTarget.TYPEALIAS)
                annotation class MyTypeAliasAnnotation

                @Target(AnnotationTarget.TYPE)
                annotation class MyTypeAnnotation

                @Target(AnnotationTarget.TYPE_PARAMETER)
                annotation class MyTypeParamAnnotation

                @MyTypeAliasAnnotation
                typealias foo = String

                val x: @MyTypeAnnotation String = "foo"

                fun <@MyTypeParamAnnotation T> foo() = 42
                """.trimIndent()
            )
        )

        "Then using a filter for a type alias annotation should visit only that annotation" {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyTypeAliasAnnotation" },
                            annotationVisitor
                        )
                    )
                )
            )

            verify {
                annotationVisitor.visitTypeAliasAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeAliasMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeAliasAnnotation"
                    }
                )
            }
        }

        "Then using a filter for a type annotation should visit only that annotation" {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyTypeAnnotation" },
                            annotationVisitor
                        )
                    )
                )
            )

            verify {
                annotationVisitor.visitTypeAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeAnnotation"
                    }
                )
            }
        }

        "Then using a filter for a type parameter annotation should visit only that annotation" {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyTypeParamAnnotation" },
                            annotationVisitor
                        )
                    )
                )
            )

            verify {
                annotationVisitor.visitTypeParameterAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeParamAnnotation"
                    }
                )
            }
        }
    }
})
