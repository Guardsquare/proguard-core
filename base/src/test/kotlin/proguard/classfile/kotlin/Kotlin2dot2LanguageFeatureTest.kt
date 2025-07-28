package proguard.classfile.kotlin

import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.BehaviorSpec
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

class KotlinLanguageFeatureTestUtil : BehaviorSpec({
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
