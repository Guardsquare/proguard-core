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
import proguard.classfile.kotlin.visitor.AllTypeVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class KotlinUnderlyingPropertyTest : FreeSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            @JvmInline
            value class Password(private val s : String)
            """.trimIndent()
        )
    )

    "Given an inline class" - {
        val clazz = programClassPool.getClass("Password")
        val kotlinClassVisitor = spyk<KotlinMetadataVisitor>()
        clazz.accept(ReferencedKotlinMetadataVisitor(kotlinClassVisitor))

        "Then its underlying property should be s" {
            val mockVisitor = spyk<KotlinMetadataVisitor>()
            clazz.kotlinMetadataAccept(mockVisitor)

            verify(exactly = 1) {
                mockVisitor.visitKotlinClassMetadata(
                    clazz,
                    withArg {
                        it.underlyingPropertyName shouldBe "s"
                    }
                )
            }
        }

        "Then its underlying property should be String" {
            val mockVisitor = spyk<KotlinMetadataVisitor>()
            clazz.kotlinMetadataAccept(mockVisitor)

            verify(exactly = 1) {
                mockVisitor.visitKotlinClassMetadata(
                    clazz,
                    withArg {
                        it.underlyingPropertyType.className shouldBe "kotlin/String"
                    }
                )
            }
        }

        "Then its underlying type should be String using AllTypeVisitor" {
            val mockVisitor = spyk<KotlinTypeVisitor>()
            clazz.kotlinMetadataAccept(AllTypeVisitor(mockVisitor))

            verify(atLeast = 1) {
                mockVisitor.visitAnyType(
                    clazz,
                    withArg {
                        it.className shouldBe "kotlin/String"
                    }
                )
            }
        }
    }
})
