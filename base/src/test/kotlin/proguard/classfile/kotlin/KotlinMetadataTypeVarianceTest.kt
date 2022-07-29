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
import proguard.classfile.kotlin.KotlinTypeVariance.IN
import proguard.classfile.kotlin.KotlinTypeVariance.INVARIANT
import proguard.classfile.kotlin.KotlinTypeVariance.OUT
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class KotlinMetadataTypeVarianceTest : FreeSpec({
    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """           
            interface FooInvariant<T>         
            interface FooIn<in T>
            interface FooOut<out T>
            """.trimIndent()
        )
    )

    "Given an interface with an invariant type parameter" - {
        val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()
        programClassPool.classesAccept(
            "FooInvariant",
            ReferencedKotlinMetadataVisitor(AllTypeParameterVisitor(typeParameterVisitor))
        )

        "Then the variance should be INVARIANT" {
            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    programClassPool.getClass("FooInvariant"),
                    withArg {
                        it.name shouldBe "T"
                        it.variance shouldBe INVARIANT
                    }
                )
            }
        }
    }

    "Given an interface with a contravariant type parameter" - {
        val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()
        programClassPool.classesAccept(
            "FooIn",
            ReferencedKotlinMetadataVisitor(AllTypeParameterVisitor(typeParameterVisitor))
        )

        "Then the variance should be IN" {
            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    programClassPool.getClass("FooIn"),
                    withArg {
                        it.name shouldBe "T"
                        it.variance shouldBe IN
                    }
                )
            }
        }
    }

    "Given an interface with a covariant type parameter" - {
        val typeParameterVisitor = spyk<KotlinTypeParameterVisitor>()
        programClassPool.classesAccept(
            "FooOut",
            ReferencedKotlinMetadataVisitor(AllTypeParameterVisitor(typeParameterVisitor))
        )

        "Then the invariance should be OUT" {
            verify(exactly = 1) {
                typeParameterVisitor.visitAnyTypeParameter(
                    programClassPool.getClass("FooOut"),
                    withArg {
                        it.name shouldBe "T"
                        it.variance shouldBe OUT
                    }
                )
            }
        }
    }
})
