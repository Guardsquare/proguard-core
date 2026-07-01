package proguard.classfile.editor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.kotlin.KotlinTypeMetadata
import proguard.classfile.kotlin.visitor.AllTypeVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.util.ClassRenamer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class KotlinAliasReferenceFixerTest : FunSpec({

    test("aliasName follows its declaring file facade when the facade is renamed to another package") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Foo.kt",
                """
                package a.b

                typealias Alias = String

                val usage: Alias = ""
                """.trimIndent(),
            ),
        )

        // Move the file facade that declares the alias to a different package.
        programClassPool.classAccept("a/b/FooKt", ClassRenamer { "x/y/FooKt" })

        // ClassReferenceFixer fixes className but, by design, leaves aliasName.
        programClassPool.classesAccept(ClassReferenceFixer(false))
        programClassPool.classesAccept(KotlinAliasReferenceFixer())

        aliasNamesIn(programClassPool).let { aliasNames ->
            aliasNames shouldContain "x/y/Alias"
            aliasNames shouldNotContain "a/b/Alias"
        }
    }

    test("aliasName follows its declaring class when the class is renamed to another package") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Foo.kt",
                """
                package a.b

                class Foo {
                    typealias Alias = String

                    val usage: Alias = ""
                }
                """.trimIndent(),
            ),
        )

        // Rename the class that declares the alias.
        programClassPool.classAccept("a/b/Foo", ClassRenamer { "x/Bar" })

        // ClassReferenceFixer fixes className but, by design, leaves aliasName.
        programClassPool.classesAccept(ClassReferenceFixer(false))
        programClassPool.classesAccept(KotlinAliasReferenceFixer())

        aliasNamesIn(programClassPool).let { aliasNames ->
            aliasNames shouldContain "x/Bar.Alias"
            aliasNames shouldNotContain "a/b/Foo.Alias"
        }
    }
})

private fun aliasNamesIn(programClassPool: ClassPool): List<String> {
    val aliasNames = mutableListOf<String>()
    programClassPool.classesAccept(
        ReferencedKotlinMetadataVisitor(
            AllTypeVisitor(
                object : KotlinTypeVisitor {
                    override fun visitAnyType(clazz: Clazz, type: KotlinTypeMetadata) {
                        type.aliasName?.let { aliasNames += it }
                    }
                },
            ),
        ),
    )
    return aliasNames
}
