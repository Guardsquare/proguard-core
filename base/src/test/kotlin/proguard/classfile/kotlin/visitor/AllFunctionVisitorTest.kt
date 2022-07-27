/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.kotlin.visitor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldExist
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.KotlinFileFacadeKindMetadata
import proguard.classfile.kotlin.KotlinFunctionMetadata
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class AllFunctionVisitorTest : FreeSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            class Foo {
                fun foo() = "hello"
                fun bar() = 42
            }

            fun foo() = "hello"
            fun bar() = 42
            """.trimIndent()
        )
    )

    "Given a class with 2 functions" - {
        val funcVisitor = spyk<KotlinFunctionVisitor>()
        val allFunctionVisitor = AllFunctionVisitor(funcVisitor)
        val fooClass = programClassPool.getClass("Foo")

        "then function `foo` and `bar` should be visited" {
            fooClass.accept(ReferencedKotlinMetadataVisitor(allFunctionVisitor))
            val slots = mutableListOf<KotlinFunctionMetadata>()

            verify(exactly = 2) {
                funcVisitor.visitFunction(fooClass, ofType(KotlinClassKindMetadata::class), capture(slots))
            }

            slots shouldExist { it.name == "foo" }
            slots shouldExist { it.name == "bar" }
        }
    }

    "Given a file facade with 2 functions" - {
        val funcVisitor = spyk<KotlinFunctionVisitor>()
        val allFunctionVisitor = AllFunctionVisitor(funcVisitor)
        val fooClass = programClassPool.getClass("TestKt")

        "then function `foo` and `bar` should be visited" {
            fooClass.accept(ReferencedKotlinMetadataVisitor(allFunctionVisitor))
            val slots = mutableListOf<KotlinFunctionMetadata>()

            verify(exactly = 2) {
                funcVisitor.visitFunction(fooClass, ofType(KotlinFileFacadeKindMetadata::class), capture(slots))
            }

            slots shouldExist { it.name == "foo" }
            slots shouldExist { it.name == "bar" }
        }
    }
})
