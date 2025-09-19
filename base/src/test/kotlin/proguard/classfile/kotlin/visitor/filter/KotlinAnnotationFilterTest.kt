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

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyAll
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.KotlinConstructorMetadata
import proguard.classfile.kotlin.KotlinFunctionMetadata
import proguard.classfile.kotlin.KotlinPropertyMetadata
import proguard.classfile.kotlin.KotlinTypeAliasMetadata
import proguard.classfile.kotlin.KotlinTypeMetadata
import proguard.classfile.kotlin.KotlinTypeParameterMetadata
import proguard.classfile.kotlin.KotlinValueParameterMetadata
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorMetadata
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import java.util.function.Predicate

class KotlinAnnotationFilterTest : BehaviorSpec({

    Given("A Kotlin file facade with annotated entities") {
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
                
                @Target(AnnotationTarget.FUNCTION)
                annotation class MyFunctionAnnotation     
                
                @Target(AnnotationTarget.VALUE_PARAMETER)
                annotation class MyValueParameterAnnotation
                
                @Target(AnnotationTarget.PROPERTY_GETTER)
                annotation class MyPropertyGetterAnnotation
                
                @Target(AnnotationTarget.PROPERTY_SETTER)
                annotation class MyPropertySetterAnnotation
                
                @Target(AnnotationTarget.PROPERTY)
                annotation class MyPropertyAnnotation
                
                @Target(AnnotationTarget.CONSTRUCTOR)
                annotation class MyConstructorAnnotation
                
                @Target(AnnotationTarget.CLASS)
                annotation class MyClassAnnotation
                
                @Target(AnnotationTarget.FIELD)
                annotation class MyFieldAnnotation
                

                @MyTypeAliasAnnotation
                typealias foo = String

                val x: @MyTypeAnnotation String = "foo"

                @MyFunctionAnnotation 
                fun <@MyTypeParamAnnotation T> foo(@MyValueParameterAnnotation valueParameter : String) = 42
                
                @MyClassAnnotation
                class Bar
                @MyConstructorAnnotation constructor(@MyValueParameterAnnotation valueParameter : String)
                
                @MyPropertyAnnotation
                var property = "initialized"               
                    @MyPropertySetterAnnotation
                    set(@MyValueParameterAnnotation valueParameter) { field = valueParameter }
                    @MyPropertyGetterAnnotation
                    get() { return field }
                    
                enum class Foo { @MyFieldAnnotation Foo }
                
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        Then("using a filter for a type alias annotation should visit only that annotation") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyTypeAliasAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitTypeAliasAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeAliasMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeAliasAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeAliasMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeAliasAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a type annotation should visit only that annotation") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyTypeAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitTypeAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a type parameter annotation should visit only that annotation") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyTypeParamAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitTypeParameterAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeParamAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinTypeParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyTypeParamAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a class annotation should visit only that annotation") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classAccept(
                "Bar",
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyClassAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitClassAnnotation(
                    programClassPool.getClass("Bar"),
                    ofType<KotlinClassKindMetadata>(),
                    withArg {
                        it.className shouldBe "MyClassAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("Bar"),
                    ofType<KotlinClassKindMetadata>(),
                    withArg {
                        it.className shouldBe "MyClassAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a constructor annotation should visit only that annotation") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classAccept(
                "Bar",
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyConstructorAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitConstructorAnnotation(
                    programClassPool.getClass("Bar"),
                    ofType<KotlinConstructorMetadata>(),
                    withArg {
                        it.className shouldBe "MyConstructorAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("Bar"),
                    ofType<KotlinConstructorMetadata>(),
                    withArg {
                        it.className shouldBe "MyConstructorAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a function annotation should visit only that annotation") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyFunctionAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitFunctionAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFunctionMetadata>(),
                    withArg {
                        it.className shouldBe "MyFunctionAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFunctionMetadata>(),
                    withArg {
                        it.className shouldBe "MyFunctionAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a property annotation should visit only those annotations") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyPropertyAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitPropertyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinPropertyMetadata>(),
                    withArg {
                        it.className shouldBe "MyPropertyAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinPropertyMetadata>(),
                    withArg {
                        it.className shouldBe "MyPropertyAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a property getter annotation should visit only those annotations") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyPropertyGetterAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitPropertyAccessorAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinPropertyAccessorMetadata>(),
                    withArg {
                        it.className shouldBe "MyPropertyGetterAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinPropertyAccessorMetadata>(),
                    withArg {
                        it.className shouldBe "MyPropertyGetterAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a property setter annotation should visit only those annotations") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyPropertySetterAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verifyAll {
                annotationVisitor.visitPropertyAccessorAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinPropertyAccessorMetadata>(),
                    withArg {
                        it.className shouldBe "MyPropertySetterAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinPropertyAccessorMetadata>(),
                    withArg {
                        it.className shouldBe "MyPropertySetterAnnotation"
                    },
                )
            }
        }

        Then("using a filter for a value parameter annotation should visit only those annotations") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        KotlinAnnotationFilter(
                            Predicate { it.className == "MyValueParameterAnnotation" },
                            annotationVisitor,
                        ),
                    ),
                ),
            )

            verify(exactly = 2) {
                annotationVisitor.visitValueParameterAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinValueParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyValueParameterAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinValueParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyValueParameterAnnotation"
                    },
                )
            }

            verify {
                annotationVisitor.visitValueParameterAnnotation(
                    programClassPool.getClass("Bar"),
                    ofType<KotlinValueParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyValueParameterAnnotation"
                    },
                )
                annotationVisitor.visitAnyAnnotation(
                    programClassPool.getClass("Bar"),
                    ofType<KotlinValueParameterMetadata>(),
                    withArg {
                        it.className shouldBe "MyValueParameterAnnotation"
                    },
                )
            }
        }
    }
})
