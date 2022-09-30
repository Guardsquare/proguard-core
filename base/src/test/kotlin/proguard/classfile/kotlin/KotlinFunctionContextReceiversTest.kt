package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.editor.ClassReferenceFixer
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.AllTypeVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataPrinter
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor
import proguard.classfile.kotlin.visitor.MultiKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter
import proguard.classfile.kotlin.visitor.filter.KotlinTypeFilter
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.ClassRenamer
import proguard.classfile.visitor.ClassCleaner
import proguard.classfile.visitor.ClassCounter
import proguard.classfile.visitor.ClassNameFilter
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.ReferencedClassVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor
import java.io.PrintWriter
import java.io.StringWriter

class KotlinFunctionContextReceiversTest : FreeSpec({
    "Given a function with context receivers" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
              context(Logger)
              fun foo() {
                  info("message")
              }
              
              class Logger {
                  fun info(message: String) {
                    println(message)
                  }
              }
    
              fun main() {
                with (Logger()) { foo() }
              }
                """.trimIndent()
            ),
            kotlincArguments = listOf("-Xcontext-receivers")
        )

        val loggerClass = programClassPool.getClass("Logger") as ProgramClass

        "When printing the metadata" - {
            val writer = StringWriter()
            programClassPool.classAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(KotlinMetadataPrinter(PrintWriter(writer)))
            )
            "Then the printed string should contain the context receiver" {
                writer.toString() shouldContain "[CTRE] Logger"
            }
        }

        "When rewriting the metadata" - {
            val writer = StringWriter()
            programClassPool.classAccept(
                "TestKt",
                ReWritingMetadataVisitor(
                    MultiKotlinMetadataVisitor(
                        // Re-initialize the references after re-writing.
                        { clazz, _ -> clazz.accept(ClassReferenceInitializer(programClassPool, libraryClassPool)) },
                        KotlinMetadataPrinter(PrintWriter(writer))
                    )
                )
            )

            "Then the printed string should contain the context receiver" {
                writer.toString() shouldContain "[CTRE] Logger"
            }
        }

        "When placing processing info on the receiver type metadata" - {
            val processingInfo = Object()

            programClassPool.getClass("TestKt").kotlinMetadataAccept(
                AllFunctionVisitor(
                    KotlinFunctionFilter({ function -> function.name == "foo" }) { clazz, kotlinMetadata, kotlinFunctionMetadata ->
                        kotlinFunctionMetadata.contextReceiverTypesAccept(clazz, kotlinMetadata) { _, kotlinTypeMetadata ->
                            kotlinTypeMetadata.processingInfo = processingInfo
                        }
                    }
                )
            )

            "Then there should be processingInfo" {
                val visitor = spyk<KotlinTypeVisitor>()
                programClassPool.getClass("TestKt").kotlinMetadataAccept(
                    AllFunctionVisitor(
                        KotlinFunctionFilter({ function -> function.name == "foo" }, AllTypeVisitor(visitor))
                    )
                )
                verify(exactly = 1) {
                    visitor.visitFunctionContextReceiverType(
                        ofType(),
                        ofType(),
                        ofType(),
                        withArg {
                            it.processingInfo shouldBe processingInfo
                        }
                    )
                }
            }

            "Then it should be cleaned using the ClassCleaner" {
                val visitor = spyk<KotlinTypeVisitor>()
                programClassPool.classAccept("TestKt", ClassCleaner())
                programClassPool.classAccept(
                    "TestKt",
                    ReferencedKotlinMetadataVisitor(
                        AllFunctionVisitor(
                            KotlinFunctionFilter({ function -> function.name == "foo" }, AllTypeVisitor(visitor))
                        )
                    )
                )
                verify(exactly = 1) {
                    visitor.visitFunctionContextReceiverType(
                        ofType(),
                        ofType(),
                        ofType(),
                        withArg {
                            it.processingInfo shouldBe null
                        }
                    )
                }
            }
        }

        "When visiting all types" - {
            val typeVisitor = spyk<KotlinTypeVisitor>()
            programClassPool.classAccept("TestKt", ReferencedKotlinMetadataVisitor(AllTypeVisitor(typeVisitor)))

            "Then the context receiver type should be visited" {
                verify(exactly = 1) {
                    typeVisitor.visitFunctionContextReceiverType(
                        programClassPool.getClass("TestKt"),
                        ofType<KotlinMetadata>(),
                        ofType<KotlinFunctionMetadata>(),
                        withArg {
                            it.className shouldBe "Logger"
                            it.referencedClass shouldBe loggerClass
                        }
                    )
                }
            }
        }

        "When visiting referenced classes" - {
            val classCounter = ClassCounter()
            programClassPool.classAccept(
                "TestKt",
                ReferencedClassVisitor(
                    false,
                    ClassNameFilter("Logger", classCounter)
                )
            )
            val numberOfReferencesWithoutKotlinMetadata = classCounter.count
            val visitor = spyk<ClassVisitor>()
            programClassPool.classAccept(
                "TestKt",
                ReferencedClassVisitor(
                    true,
                    ClassNameFilter("Logger", visitor)
                )
            )
            "Then the context receiver class should be visited" {
                // There should be at least one more visit when taking
                // into account Kotlin metadata references,
                // since the metadata now references Logger.
                verify(atLeast = numberOfReferencesWithoutKotlinMetadata + 1) {
                    visitor.visitProgramClass(loggerClass)
                }
            }
        }

        "When visiting filtered types" - {
            val typeVisitor = spyk<KotlinTypeVisitor>()
            programClassPool.classAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(
                    AllTypeVisitor(KotlinTypeFilter({ type -> type.className == "Logger" }, typeVisitor))
                )
            )

            "Then the context receiver type should be visited" {
                verify(exactly = 1) {
                    typeVisitor.visitFunctionContextReceiverType(
                        programClassPool.getClass("TestKt"),
                        ofType<KotlinMetadata>(),
                        ofType<KotlinFunctionMetadata>(),
                        withArg {
                            it.className shouldBe "Logger"
                            it.referencedClass shouldBe loggerClass
                        }
                    )
                }
            }
        }

        "When visiting context receivers" - {
            val typeVisitor = spyk<KotlinTypeVisitor>()
            programClassPool.classAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(
                    AllFunctionVisitor(
                        object : KotlinFunctionVisitor {
                            override fun visitAnyFunction(clazz: Clazz, kotlinMetadata: KotlinMetadata, kotlinFunctionMetadata: KotlinFunctionMetadata) {
                                kotlinFunctionMetadata.contextReceiverTypesAccept(clazz, kotlinMetadata, typeVisitor)
                            }
                        }
                    )
                )
            )
            "Then the visit method should be called with the correct type information" {
                verify(exactly = 1) {
                    typeVisitor.visitFunctionContextReceiverType(
                        programClassPool.getClass("TestKt"),
                        ofType<KotlinMetadata>(),
                        ofType<KotlinFunctionMetadata>(),
                        withArg {
                            it.className shouldBe "Logger"
                            it.referencedClass shouldBe loggerClass
                        }
                    )
                }
            }
        }

        "When renaming a context receiver class and fixing the references" - {
            programClassPool.classesAccept("Logger", ClassRenamer { "ObfuscatedLogger" })
            programClassPool.classesAccept(ClassReferenceFixer(false))

            val typeVisitor = spyk<KotlinTypeVisitor>()
            programClassPool.classAccept(
                "TestKt",
                ReferencedKotlinMetadataVisitor(
                    AllFunctionVisitor(
                        object : KotlinFunctionVisitor {
                            override fun visitAnyFunction(clazz: Clazz, kotlinMetadata: KotlinMetadata, kotlinFunctionMetadata: KotlinFunctionMetadata) {
                                kotlinFunctionMetadata.contextReceiverTypesAccept(clazz, kotlinMetadata, typeVisitor)
                            }
                        }
                    )
                )
            )
            "Then the visit method should be called with the correct type information" {
                verify(exactly = 1) {
                    typeVisitor.visitFunctionContextReceiverType(
                        programClassPool.getClass("TestKt"),
                        ofType<KotlinMetadata>(),
                        ofType<KotlinFunctionMetadata>(),
                        withArg {
                            it.className shouldBe "ObfuscatedLogger"
                            it.referencedClass shouldBe loggerClass
                        }
                    )
                }
            }
        }
    }
})
