/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeParameterVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class KotlinMetadataTypeVarianceTest : FreeSpec({
    val programClassPool = ClassPoolBuilder.fromSource(
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
                        it.variance shouldBe KotlinTypeVariance.INVARIANT
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
                        it.variance shouldBe KotlinTypeVariance.IN
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
                        it.variance shouldBe KotlinTypeVariance.OUT
                    }
                )
            }
        }
    }
})
