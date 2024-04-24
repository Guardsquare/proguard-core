package proguard.util.kotlin.asserter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import org.apache.logging.log4j.LogManager
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.kotlin.KotlinClassKindMetadata
import proguard.classfile.kotlin.KotlinMetadata
import proguard.classfile.kotlin.KotlinMultiFileFacadeKindMetadata
import proguard.classfile.kotlin.visitor.KotlinMetadataRemover
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.WarningLogger
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.resources.file.ResourceFilePool
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class KotlinMetadataAsserterTest : BehaviorSpec({
    val warningLogger = WarningLogger(LogManager.getLogger(KotlinMetadataAsserter::class.java))
    Given("an interface with default implementation") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                interface Test {
                    fun foo() {
                        println("DEFAULT")
                    }
                }
                """.trimIndent(),
            ),
        )

        When("the KotlinMetadataAsserter is run") {

            // Remove invalid kotlin metadata
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
            Then("the metadata should not be thrown away") {
                val visitor = spyk<KotlinMetadataVisitor>()
                programClassPool.classesAccept("Test") {
                    it.kotlinMetadataAccept(visitor)
                }

                verify(exactly = 1) {
                    visitor.visitKotlinClassMetadata(
                        programClassPool.getClass("Test"),
                        ofType<KotlinClassKindMetadata>(),
                    )
                }
            }
        }
    }

    Given("an interface with default implementation and missing \$DefaultImpls class") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                interface Test {
                    fun foo() {
                        println("DEFAULT")
                    }
                }
                """.trimIndent(),
            ),
        )

        When("the KotlinMetadataAsserter is run") {

            programClassPool.removeClass("Test\$DefaultImpls")

            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())

            Then("the metadata should be thrown away") {
                val visitor = spyk<KotlinMetadataVisitor>()
                programClassPool.classesAccept("Test") {
                    it.kotlinMetadataAccept(visitor)
                }

                verify(exactly = 0) {
                    visitor.visitKotlinClassMetadata(
                        programClassPool.getClass("Test"),
                        ofType<KotlinClassKindMetadata>(),
                    )
                }
            }
        }
    }

    Given("an interface with default implementation using Java 8+ default methods") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                interface Test {
                    fun foo() {
                        println("DEFAULT")
                    }
                }
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xjvm-default=all"),
        )

        When("the KotlinMetadataAsserter is run") {
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
            Then("the metadata should not be thrown away") {
                val visitor = spyk<KotlinMetadataVisitor>()
                programClassPool.classesAccept("Test") {
                    it.kotlinMetadataAccept(visitor)
                }

                verify(exactly = 1) {
                    visitor.visitKotlinClassMetadata(
                        programClassPool.getClass("Test"),
                        ofType<KotlinClassKindMetadata>(),
                    )
                }
            }
        }
    }

    Given("an enum") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                enum class Test {
                    CENTER,
                    BOTTOM
                }
                """.trimIndent(),
            ),
        )

        When("the referencedEnumEntries are set to null") {
            val visitor = spyk(
                object : KotlinMetadataVisitor {
                    override fun visitAnyKotlinMetadata(clazz: Clazz?, kotlinMetadata: KotlinMetadata?) {}

                    override fun visitKotlinClassMetadata(
                        clazz: Clazz?,
                        kotlinClassKindMetadata: KotlinClassKindMetadata?,
                    ) {
                        kotlinClassKindMetadata?.referencedEnumEntries = listOf(null, null)
                    }
                },
            )
            programClassPool.classesAccept("Test") {
                it.kotlinMetadataAccept(visitor)
            }

            Then("the KotlinMetadataAsserter should throw away the enum metadata") {
                (programClassPool.getClass("Test") as ProgramClass).kotlinMetadata shouldNotBe null
                // KotlinMetadataAsserter should remove Test enum's metadata because
                // null entries of kotlinClassKindMetadata.referencedEnumEntries violates the ClassIntegrity.
                KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
                (programClassPool.getClass("Test") as ProgramClass).kotlinMetadata shouldBe null
            }
        }
    }

    Given("a user of a type alias for a library class") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "ATest.kt",
                """
                class ATest : MyAlias()
                """.trimIndent(),
            ),
            KotlinSource(
                "FileFacade.kt",
                """
                 typealias MyAlias = Exception
                """.trimIndent(),
            ),
        )

        When("the library class has no metadata") {
            // Simulate missing metadata by removing the metadata
            // from the library classes and re-initialize.
            with(KotlinMetadataInitializer(warningLogger)) {
                libraryClassPool.classesAccept(KotlinMetadataRemover())
                programClassPool.classesAccept(this)
                libraryClassPool.classesAccept(this)
                programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))
            }

            // Run the asserter to remove invalid metadata.
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())

            // Re-initialize after running the asserter.
            programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

            Then("the asserter should remove metadata for file facade where the alias is defined") {
                (programClassPool.getClass("FileFacadeKt") as ProgramClass).kotlinMetadata shouldBe null
            }

            Then("the asserter should remove metadata for the class where the alias is used") {
                // Since the metadata where the alias is declared is now invalid,
                // ATest references now invalid metadata and is itself invalid.
                (programClassPool.getClass("ATest") as ProgramClass).kotlinMetadata shouldBe null
            }
        }
    }

    Given("multi file class facade metadata") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "partOne.kt",
                """
                        @file:JvmMultifileClass
                        @file:JvmName("MultiFileClass")
                        fun partOne(): String = "one"
                """.trimIndent(),
            ),
        )
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        When("the facade does not reference itself as a part") {
            val facadeClass = programClassPool.getClass("MultiFileClass") as ProgramClass

            Then("metadata should not be thrown away") {
                KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
                programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))
                facadeClass.kotlinMetadata shouldNotBe null
            }
        }

        When("the facade references itself as a part") {
            val facadeClass = programClassPool.getClass("MultiFileClass") as ProgramClass
            facadeClass.kotlinMetadataAccept { clazz, kotlinMetadata ->
                if (kotlinMetadata is KotlinMultiFileFacadeKindMetadata) {
                    kotlinMetadata.referencedPartClasses.add(clazz)
                    kotlinMetadata.partClassNames.add(clazz.getName())
                }
            }

            Then("metadata should be thrown away") {
                KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
                programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))
                facadeClass.kotlinMetadata shouldBe null
            }
        }
    }

    Given("multi file class facade metadata with null references") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "partOne.kt",
                """
                        @file:JvmMultifileClass
                        @file:JvmName("MultiFileClass")
                        fun partOne(): String = "one"
                """.trimIndent(),
            ),
            KotlinSource(
                "partTwo.kt",
                """
                        @file:JvmMultifileClass
                        @file:JvmName("MultiFileClass")
                        fun partTwo(): String = "two"
                """.trimIndent(),
            ),
        )

        val facadeClass = programClassPool.getClass("MultiFileClass") as ProgramClass
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        Then("Before creating the null reference the metadata is not removed") {
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
            facadeClass.kotlinMetadata shouldNotBe null
        }

        // We remove one of the classes from the class pool and reinitialize, to generate a null reference.
        programClassPool.removeClass("MultiFileClass__PartTwoKt")
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        Then("The asserter should remove the metadata") {
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
            facadeClass.kotlinMetadata shouldBe null
        }
    }

    Given("multi file class facade metadata with a dangling class reference") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "partOne.kt",
                """
                        @file:JvmMultifileClass
                        @file:JvmName("MultiFileClass")
                        fun partOne(): String = "one"
                """.trimIndent(),
            ),
            KotlinSource(
                "partTwo.kt",
                """
                        @file:JvmMultifileClass
                        @file:JvmName("MultiFileClass")
                        fun partTwo(): String = "two"
                """.trimIndent(),
            ),
        )

        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))
        val facadeClass = programClassPool.getClass("MultiFileClass") as ProgramClass

        Then("Before introducing a dangling reference the metadata isn't removed") {
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
            facadeClass.kotlinMetadata shouldNotBe null
        }

        // We remove one of the classes from the class pool after initialization, to generate a dangling class reference.
        programClassPool.removeClass("MultiFileClass__PartTwoKt")

        Then("The asserter should remove the metadata") {
            KotlinMetadataAsserter().execute(warningLogger, programClassPool, libraryClassPool, ResourceFilePool())
            facadeClass.kotlinMetadata shouldBe null
        }
    }
})
