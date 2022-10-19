package proguard.classfile.editor

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.ClassPool
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.util.ClassRenamer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class ClassReferenceFixerKotlinCompanionTest : FunSpec({

    include(
        "Default companion name: ",
        testCompanion(
            ClassPoolBuilder.fromSource(
                KotlinSource(
                    "Foo.kt",
                    """
                    class Foo {
                        companion object
                    }
                    """.trimIndent()
                )
            ).programClassPool,
            originalName = "Foo${'$'}Companion"
        )
    )

    include(
        "Named companion: ",
        testCompanion(
            ClassPoolBuilder.fromSource(
                KotlinSource(
                    "Foo.kt",
                    """
                    class Foo {
                        companion object MyCompanion
                    }
                    """.trimIndent()
                )
            ).programClassPool,
            originalName = "Foo${'$'}MyCompanion"
        )
    )

    include(
        "Named companion starting with a dollar: ",
        testCompanion(
            ClassPoolBuilder.fromSource(
                KotlinSource(
                    "Foo.kt",
                    """
                class Foo {
                    companion object `${'$'}MyCompanion`
                }
                    """.trimIndent()
                )
            ).programClassPool,
            originalName = "Foo${'$'}${'$'}MyCompanion"
        )
    )

    include(
        "Named companion containing a dollar: ",
        testCompanion(
            ClassPoolBuilder.fromSource(
                KotlinSource(
                    "Foo.kt",
                    """
                class Foo {
                    companion object `My${'$'}Companion`
                }
                    """.trimIndent()
                )
            ).programClassPool,
            originalName = "Foo${'$'}My${'$'}Companion"
        )
    )
})

private fun testCompanion(programClassPool: ClassPool, originalName: String) = funSpec {
    val newName = "Foo${'$'}ObfuscatedCompanion"
    val newShortName = "ObfuscatedCompanion"

    test("The companion name should be updated correctly") {
        programClassPool.classAccept(
            originalName,
            ClassRenamer {
                newName
            }
        )

        programClassPool.classesAccept(ClassReferenceFixer(false))

        val visitor = spyk<KotlinMetadataVisitor>()
        programClassPool.classAccept(
            "Foo",
            ReferencedKotlinMetadataVisitor(visitor)
        )
        verify {
            visitor.visitKotlinClassMetadata(
                programClassPool.getClass("Foo"),
                withArg {
                    it.companionObjectName shouldBe newShortName
                }
            )
        }
    }
}
