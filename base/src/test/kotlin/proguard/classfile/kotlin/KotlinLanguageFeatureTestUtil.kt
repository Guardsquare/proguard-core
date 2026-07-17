package proguard.classfile.kotlin

import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.equals.shouldBeEqual
import proguard.classfile.kotlin.visitor.KotlinMetadataPrinter
import proguard.classfile.kotlin.visitor.MultiKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.util.ClassReferenceInitializer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor
import java.io.PrintWriter
import java.io.StringWriter

class KotlinLanguageFeatureTestUtil : FunSpec({
    include(
        shouldRewriteMetadataCorrectly(
            "Non-local break & continue",
            KotlinSource(
                "Test.kt",
                """
                fun processList(elements: List<Int>): Boolean {
                    for (element in elements) {
                        val variable = element ?: run {
                            continue
                        }
                        if (variable == 0) return true
                    }
                    return false
                }
                """.trimIndent(),
            ),
        ),
    )
    include(
        shouldRewriteMetadataCorrectly(
            "Nested type aliases",
            KotlinSource(
                "Test.kt",
                """
                class Container {
                    typealias ContainerSet = Set<Container>
                }

                """.trimIndent(),
            ),
            listOf("-Xnested-type-aliases"),
        ),
    )
    include(
        shouldRewriteMetadataCorrectly(
            "RequiresOptIn annotations",
            KotlinSource(
                "Test.kt",
                """
                @RequiresOptIn(
                level = RequiresOptIn.Level.WARNING,
                message = "Interfaces in this library are experimental"
                )
                annotation class UnstableApi()

                @SubclassOptInRequired(UnstableApi::class)
                interface CoreLibraryApi
                """.trimIndent(),
            ),
        ),
    )
    include(
        shouldRewriteMetadataCorrectly(
            "Guard conditions in when with a subject",
            KotlinSource(
                "Test.kt",
                """
                sealed interface Animal {
                    data class Cat(val mouseHunter: Boolean) : Animal {
                        fun feedCat() {}
                    }

                    data class Dog(val breed: String) : Animal {
                        fun feedDog() {}
                    }
                }

                fun feedAnimal(animal: Animal) {
                    when (animal) {
                        // Branch with only the primary condition. Calls `feedDog()` when `animal` is `Dog`
                        is Animal.Dog -> animal.feedDog()
                        // Branch with both primary and guard conditions. Calls `feedCat()` when `animal` is `Cat` and is not `mouseHunter`
                        is Animal.Cat if !animal.mouseHunter -> animal.feedCat()
                        // Prints "Unknown animal" if none of the above conditions match
                        else -> println("Unknown animal")
                    }
                }
                """.trimIndent(),
            ),
        ),
    )
    include(
        shouldRewriteMetadataCorrectly(
            "Multi-dollar string interpolation",
            KotlinSource(
                "Test.kt",
                """
                class MultiDollar {
                    companion object {
                        val multiDollarString: String
                              get() = ${"$$"}${"\"\"\""}aMultiDollarString${"\"\"\""}
                    }
                }
                """.trimIndent(),
            ),
        ),
    )

    // Kotlin language features added in Kotlin 2.4.

    include(
        shouldRewriteMetadataCorrectly(
            "Backing fields",
            KotlinSource(
                "Test.kt",
                """
                    class ShoppingCart {
                        // Public read-only view with explicit backing field
                        val items: List<String>
                            field = mutableListOf()

                        fun addItem(item: String) {
                            items.add(item)
                        }

                        fun removeItem(item: String) {
                            items.remove(item)
                        }
                    }

                """.trimIndent(),
            ),
        ),
    )

    include(
        shouldRewriteMetadataCorrectly(
            "Experimental Explicit context arguments for context parameters",
            KotlinSource(
                "Test.kt",
                """
                     class EmailSender
                        class SmsSender
    
                    context(emailSender: EmailSender)
                    fun sendNotification() {
                        println("Sent email notification")
                    }
    
                    context(smsSender: SmsSender)
                    fun sendNotification() {
                        println("Sent SMS notification")
                    }
    
                    context(defaultEmailSender: EmailSender, defaultSmsSender: SmsSender)
                    fun notifyUser() {
    
                        // Selects the overload with the EmailSender context parameter
                        sendNotification(emailSender = defaultEmailSender)
    
                        // Selects the overload with the SmsSender context parameter
                        sendNotification(smsSender = defaultSmsSender)
                    }
                """.trimIndent(),
            ),
            listOf("-Xexplicit-context-arguments"),
        ),
    )

    include(
        shouldRewriteMetadataCorrectly(
            "Experimental collection literals",
            KotlinSource(
                "Test.kt",
                """
                     val fruit = ["apple", "banana", "cherry"]

                """.trimIndent(),
            ),
            listOf("-Xcollection-literals"),
        ),
    )

    include(
        shouldRewriteMetadataCorrectly(
            "New API for converting unsigned integers to BigInteger",
            KotlinSource(
                "Test.kt",
                """
                    val unsignedLong = Long.MAX_VALUE.toULong() + 1uL
                    val unsignedInt = UInt.MAX_VALUE
                """.trimIndent(),
            ),
        ),
    )

    include(
        shouldRewriteMetadataCorrectly(
            "Checking sorted order",
            KotlinSource(
                "Test.kt",
                """
                        data class User(val name: String, val age: Int)
                        
                        fun main() {
                            val numbers = listOf(1, 2, 3, 4)
                            println(numbers.isSorted())
                            // true
                        
                            val users = listOf(
                                User("Alice", 24),
                                User("Bob", 31),
                                User("Charlie", 29),
                            )
                            println(users.isSortedBy(User::age))
                            // false
                        }

                """.trimIndent(),
            ),
        ),
    )
})

/**
 * Compiles the provided Kotlin source and verifies that the original & rewritten metadata are the same.
 */
fun shouldRewriteMetadataCorrectly(
    testName: String,
    kotlinSource: KotlinSource,
    compilerArgs: List<String> = listOf(),
): TestFactory = funSpec {
    test(testName) {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            kotlinSource,
            kotlincArguments = compilerArgs,
        )

        val writer = StringWriter()
        val rewriter = StringWriter()

        programClassPool.classesAccept(
            ReferencedKotlinMetadataVisitor(
                KotlinMetadataPrinter(PrintWriter(writer)),
            ),
        )

        programClassPool.classesAccept(
            ReWritingMetadataVisitor(
                MultiKotlinMetadataVisitor(
                    { clazz, _ ->
                        clazz.accept(
                            ClassReferenceInitializer(
                                programClassPool,
                                libraryClassPool,
                            ),
                        )
                    },
                    KotlinMetadataPrinter(PrintWriter(rewriter)),
                ),
            ),
        )

        writer.toString() shouldBeEqual rewriter.toString()
    }
}
