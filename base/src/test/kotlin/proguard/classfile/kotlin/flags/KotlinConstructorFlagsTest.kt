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
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.visitor.AllConstructorVisitor
import proguard.classfile.kotlin.visitor.KotlinConstructorVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinConstructorFilter
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

class KotlinConstructorFlagsTest : BehaviorSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            // The tests will identify the constructors by the number of params.
            @Suppress("UNUSED_PARAMETER")
            class Foo constructor(param: String) {
                constructor(param: String, param2: String) : this(param) { }
                constructor(param: String, param2: String, param3: String) : this(param) { }
                constructor(param: String, param2: String, param3: String, param4: String) : this(param) { }
                constructor(param: String, param2: String, param3: String, param4: String, param5: String) : this(param) { }
            }
            annotation class Ann
            class Bar {
                @Ann constructor(param: String)
            }

            """.trimIndent(),
        ),
    )

    Given("a primary constructor") {
        val clazz = programClassPool.getClass("Foo")

        Then("the flags should be initialized as expected") {
            val consVisitor = spyk<KotlinConstructorVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(consVisitor, secondary = false)))

            verify(exactly = 1) {
                consVisitor.visitConstructor(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        withClue("Init: isSecondary shouldBe false") {
                            it.flags.isSecondary shouldBe false
                        }
                        withClue("Init: hasAnnotationsInBytecode shouldBe false") {
                            it.flags.hasAnnotationsInBytecode shouldBe false
                        }
                    },
                )
            }
        }

        Then("the flags should be written and re-initialized as expected") {
            val consVisitor = spyk<KotlinConstructorVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor(consVisitor, secondary = false)))

            verify(exactly = 1) {
                consVisitor.visitConstructor(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        withClue("Rewrite: isSecondary shouldBe false") {
                            it.flags.isSecondary shouldBe false
                        }
                        withClue("Init: hasAnnotationsInBytecode shouldBe false") {
                            it.flags.hasAnnotationsInBytecode shouldBe false
                        }
                    },
                )
            }
        }
    }

    Given("secondary constructors") {
        val clazz = programClassPool.getClass("Foo")

        Then("the flags should be initialized as expected") {
            val consVisitor = spyk<KotlinConstructorVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(createVisitor(consVisitor, secondary = true)))

            verify(exactly = 4) {
                consVisitor.visitConstructor(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        withClue("Init: isSecondary shouldBe true") {
                            it.flags.isSecondary shouldBe true
                        }
                        withClue("Init: hasAnnotationsInBytecode shouldBe false") {
                            it.flags.hasAnnotationsInBytecode shouldBe false
                        }
                    },
                )
            }
        }

        Then("the flags should be written and re-initialized as expected") {
            val consVisitor = spyk<KotlinConstructorVisitor>()

            clazz.accept(ReWritingMetadataVisitor(createVisitor(consVisitor, secondary = true)))

            verify(exactly = 4) {
                consVisitor.visitConstructor(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        withClue("Rewrite: isSecondary shouldBe true") {
                            it.flags.isSecondary shouldBe true
                        }
                        withClue("Init: hasAnnotationsInBytecode shouldBe false") {
                            it.flags.hasAnnotationsInBytecode shouldBe false
                        }
                    },
                )
            }
        }
    }

    Given("a constructor with an annotation") {
        val clazz = programClassPool.getClass("Bar")

        Then("the flags should be initialized as expected") {
            val consVisitor = spyk<KotlinConstructorVisitor>()

            clazz.accept(ReferencedKotlinMetadataVisitor(AllConstructorVisitor(consVisitor)))

            verify(exactly = 1) {
                consVisitor.visitConstructor(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        withClue("Init: isSecondary shouldBe true") {
                            it.flags.isSecondary shouldBe true
                        }
                        withClue("Init: hasAnnotationsInBytecode shouldBe false") {
                            it.flags.hasAnnotationsInBytecode shouldBe true
                        }
                    },
                )
            }
        }

        Then("the flags should be written and re-initialized as expected") {
            val consVisitor = spyk<KotlinConstructorVisitor>()

            clazz.accept(ReWritingMetadataVisitor(AllConstructorVisitor(consVisitor)))

            verify(exactly = 1) {
                consVisitor.visitConstructor(
                    clazz,
                    ofType(KotlinClassKindMetadata::class),
                    withArg {
                        withClue("Rewrite: isSecondary shouldBe true") {
                            it.flags.isSecondary shouldBe true
                        }
                        withClue("Init: hasAnnotationsInBytecode shouldBe false") {
                            it.flags.hasAnnotationsInBytecode shouldBe true
                        }
                    },
                )
            }
        }
    }

    // TODO(T5550) Add test for hasNonStableParameterNames
})

private fun createVisitor(consVisitor: KotlinConstructorVisitor, secondary: Boolean): KotlinMetadataVisitor =
    AllConstructorVisitor(
        KotlinConstructorFilter(
            { if (!secondary) it.valueParameters.size == 1 else it.valueParameters.size > 1 },
            consVisitor,
        ),
    )
