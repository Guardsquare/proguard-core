package proguard.classfile.util

import io.kotest.core.spec.style.FreeSpec
import io.mockk.mockk
import io.mockk.verify
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class ClassReferenceInitializerKotlinInterfaceTest : FreeSpec({
    "Given an interface with default implementation" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                interface Test {
                    fun foo() {
                        println("DEFAULT")
                    }
                }
                """.trimIndent()
            )
        )

        "When the ClassReferenceInitializer is run" - {
            val missingClassPrinter = mockk<WarningPrinter>(relaxed = true)
            val classReferenceInitializer = ClassReferenceInitializer(programClassPool, libraryClassPool, missingClassPrinter, null, null, null)

            "Then a missing reference should not be reported" {
                programClassPool.classesAccept("Test", classReferenceInitializer)

                verify(exactly = 0) {
                    missingClassPrinter.print(
                        "Test",
                        "Test${'$'}DefaultImpls",
                        ofType<String>()
                    )
                }
            }
        }
    }

    "Given an interface with default implementation using Java 8+ default methods" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                interface Test {
                    fun foo() {
                        println("DEFAULT")
                    }
                }
                """.trimIndent()
            ),
            kotlincArguments = listOf("-Xjvm-default=all")
        )

        "When the ClassReferenceInitializer is run" - {
            val missingClassPrinter = mockk<WarningPrinter>(relaxed = true)
            val classReferenceInitializer = ClassReferenceInitializer(programClassPool, libraryClassPool, missingClassPrinter, null, null, null)

            "Then a missing reference should not be reported" {
                programClassPool.classesAccept("Test", classReferenceInitializer)

                verify(exactly = 0) {
                    missingClassPrinter.print(
                        "Test",
                        "Test${'$'}DefaultImpls",
                        ofType<String>()
                    )
                }
            }
        }
    }
})
