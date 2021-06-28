/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.kotlin.KotlinAnnotation
import proguard.classfile.kotlin.KotlinTypeMetadata
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class ClassReferenceInitializerTest : FreeSpec({

    "Kotlin annotations should be initialized correctly" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.TYPE)
                annotation class MyTypeAnnotation(
                    val string: String,
                    val byte: Byte,
                    val char: Char,
                    val short: Short,
                    val int: Int,
                    val long: Long,
                    val float: Float,
                    val double: Double,
                    val boolean: Boolean,
                    val uByte: UByte,
                    val uShort: UShort,
                    val uInt: UInt,
                    val uLong: ULong,
                    val kClass: KClass<*>,
                    val enum: MyEnum,
                    val array: Array<String>,
                    val annotation: Foo
                )

                val x: @MyTypeAnnotation(
                    string = "foo",
                    byte = 1,
                    char = 'a',
                    short = 1,
                    int = 1,
                    long = 1L,
                    float = 1f,
                    double = 1.0,
                    boolean = true,
                    uByte = 1u,
                    uShort = 1u,
                    uInt = 1u,
                    uLong = 1uL,
                    kClass = String::class,
                    enum = MyEnum.FOO,
                    array = arrayOf("foo", "bar"),
                    annotation = Foo("foo")) String = "foo"

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent()
            ),
            kotlincArguments = listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
        )

        // Note: the `ClassReferenceInitializer` is called by `ClassPoolBuilder.fromSource`
        // when creating a class pool, so no need to call it again in the test.

        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(ReferencedKotlinMetadataVisitor(AllKotlinAnnotationVisitor(annotationVisitor)))
        val annotation = slot<KotlinAnnotation>()

        "there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeAnnotation(fileFacadeClass, ofType(KotlinTypeMetadata::class), capture(annotation)) }
        }

        "the annotation referenced class should be correct" {
            annotation.captured.className shouldBe "MyTypeAnnotation"
            annotation.captured.referencedAnnotationClass shouldBe programClassPool.getClass("MyTypeAnnotation")
        }

        "the annotation field references should be correctly set" {
            with(programClassPool.getClass("MyTypeAnnotation")) {
                annotation.captured.referencedArgumentMethods shouldBe mapOf(
                    "string" to findMethod("string", "()Ljava/lang/String;"),
                    "byte" to findMethod("byte", "()B"),
                    "char" to findMethod("char", "()C"),
                    "short" to findMethod("short", "()S"),
                    "int" to findMethod("int", "()I"),
                    "long" to findMethod("long", "()J"),
                    "float" to findMethod("float", "()F"),
                    "double" to findMethod("double", "()D"),
                    "boolean" to findMethod("boolean", "()Z"),
                    "uByte" to findMethod("uByte", "()B"),
                    "uShort" to findMethod("uShort", "()S"),
                    "uInt" to findMethod("uInt", "()I"),
                    "uLong" to findMethod("uLong", "()J"),
                    "enum" to findMethod("enum", "()LMyEnum;"),
                    "array" to findMethod("array", "()[Ljava/lang/String;"),
                    "annotation" to findMethod("annotation", "()LFoo;"),
                    "kClass" to findMethod("kClass", "()Ljava/lang/Class;")
                )
            }
        }
    }
})
