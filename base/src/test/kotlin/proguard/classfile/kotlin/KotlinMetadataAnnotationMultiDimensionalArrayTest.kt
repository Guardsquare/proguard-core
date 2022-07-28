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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinAnnotationArgument.ClassValue
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import testutils.ReWritingMetadataVisitor

class KotlinMetadataAnnotationMultiDimensionalArrayTest : FreeSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            import kotlin.reflect.KClass

            @Target(AnnotationTarget.TYPEALIAS)
            annotation class MyTypeAliasAnnotation(val kClass: KClass<*>)

            @MyTypeAliasAnnotation(kClass = String::class)
            typealias myAlias = String

            @MyTypeAliasAnnotation(kClass = Array<String>::class)
            typealias myAlias1 = String

            @MyTypeAliasAnnotation(kClass = Array<Array<String>>::class)
            typealias myAlias2 = String

            @MyTypeAliasAnnotation(kClass = Array<Array<Array<String>>>::class)
            typealias myAlias3 = String
            """.trimIndent()
        ),
        kotlincArguments = listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
    )

    "Given a type alias with an annotation with multi-dimensional array values" - {
        val fileFacadeClass = programClassPool.getClass("TestKt")
        "Then the argument values should be correct" {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                fileFacadeClass.name,
                ReWritingMetadataVisitor(
                    AllTypeAliasVisitor(
                        AllKotlinAnnotationVisitor(
                            AllKotlinAnnotationArgumentVisitor(annotationArgVisitor)
                        )
                    )
                )
            )

            verify(exactly = 1) {
                annotationArgVisitor.visitClassArgument(
                    fileFacadeClass,
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg {
                        it.name shouldBe "kClass"
                    },
                    ClassValue("kotlin/String", 0)
                )

                annotationArgVisitor.visitClassArgument(
                    fileFacadeClass,
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg {
                        it.name shouldBe "kClass"
                    },
                    ClassValue("kotlin/String", 1)
                )
                annotationArgVisitor.visitClassArgument(
                    fileFacadeClass,
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg {
                        it.name shouldBe "kClass"
                    },
                    ClassValue("kotlin/String", 2)
                )
                annotationArgVisitor.visitClassArgument(
                    fileFacadeClass,
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg {
                        it.name shouldBe "kClass"
                    },
                    ClassValue("kotlin/String", 3)
                )
            }
        }
    }
})
