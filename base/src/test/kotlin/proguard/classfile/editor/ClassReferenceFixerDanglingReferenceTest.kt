package proguard.classfile.editor

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import proguard.classfile.util.ClassReferenceInitializer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

/**
 * Regression tests for https://github.com/Guardsquare/proguard-core/issues/174.
 *
 * When the class pool is incomplete -- e.g. a class referenced only from `@Metadata` isn't on the
 * classpath -- [ClassReferenceInitializer] leaves the matching `referenced*` field null.
 * [ClassReferenceFixer] must handle this case gracefully rather than throwing a
 * [NullPointerException] while fixing the Kotlin metadata.
 */
class ClassReferenceFixerDanglingReferenceTest : FunSpec({

    test("A dangling Kotlin type reference does not crash the ClassReferenceFixer") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Holder.kt",
                """
                class Referenced

                class Holder {
                    val held: Referenced = Referenced()
                    fun make(): Referenced = Referenced()
                }
                """.trimIndent(),
            ),
        )

        // Simulate an incomplete class pool: remove the referenced class and reinitialize so
        // Holder's Kotlin type metadata is left with null referencedClass fields.
        programClassPool.removeClass("Referenced")
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        shouldNotThrowAny {
            programClassPool.classesAccept(ClassReferenceFixer(false))
        }
    }

    test("A dangling Kotlin annotation-argument reference does not crash the ClassReferenceFixer") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Holder.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.TYPE)
                annotation class Anno(val cls: KClass<*>)

                class Referenced

                class Holder {
                    val annotated: @Anno(Referenced::class) String = ""
                }
                """.trimIndent(),
            ),
        )

        // Simulate an incomplete class pool: remove the class referenced by the annotation argument
        // and reinitialize so the KClass argument's referencedClass is left null.
        programClassPool.removeClass("Referenced")
        programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, libraryClassPool))

        shouldNotThrowAny {
            programClassPool.classesAccept(ClassReferenceFixer(false))
        }
    }
})
