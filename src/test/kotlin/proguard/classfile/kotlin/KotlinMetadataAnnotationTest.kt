/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument.AnnotationValue
import kotlinx.metadata.KmAnnotationArgument.ArrayValue
import kotlinx.metadata.KmAnnotationArgument.BooleanValue
import kotlinx.metadata.KmAnnotationArgument.ByteValue
import kotlinx.metadata.KmAnnotationArgument.CharValue
import kotlinx.metadata.KmAnnotationArgument.DoubleValue
import kotlinx.metadata.KmAnnotationArgument.EnumValue
import kotlinx.metadata.KmAnnotationArgument.FloatValue
import kotlinx.metadata.KmAnnotationArgument.IntValue
import kotlinx.metadata.KmAnnotationArgument.KClassValue
import kotlinx.metadata.KmAnnotationArgument.LongValue
import kotlinx.metadata.KmAnnotationArgument.ShortValue
import kotlinx.metadata.KmAnnotationArgument.StringValue
import kotlinx.metadata.KmAnnotationArgument.UByteValue
import kotlinx.metadata.KmAnnotationArgument.UIntValue
import kotlinx.metadata.KmAnnotationArgument.ULongValue
import kotlinx.metadata.KmAnnotationArgument.UShortValue
import proguard.classfile.kotlin.visitor.AllFunctionsVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import java.util.function.Predicate

class KotlinMetadataAnnotationTest : FreeSpec({

    val programClassPool = ClassPoolBuilder.fromSource(
        KotlinSource(
            "Test.kt",
            """
            import kotlin.reflect.KClass

            // Declare 3 annotations targeting: type aliases, types, type parameters -
            // these are all stored in the Kotlin metadata rather than as Java annotations.
            @Target(AnnotationTarget.TYPEALIAS)
            annotation class MyTypeAliasAnnotation(
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

            @Target(AnnotationTarget.TYPE)
            annotation class MyTypeAnnotation

            @Target(AnnotationTarget.TYPE_PARAMETER)
            annotation class MyTypeParamAnnotation

            // Use the 3 annotations

            @MyTypeAliasAnnotation(
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
                annotation = Foo("foo"))
            typealias myAlias = String

            val x: @MyTypeAnnotation String = "foo"

            fun <@MyTypeParamAnnotation T> foo() = 42

            // extra helpers

            enum class MyEnum { FOO, BAR }
            annotation class Foo(val string: String)
            """.trimIndent()
        ),
        kotlincArguments = listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
    )

    "Given a type alias with 1 annotation" - {
        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)
        val fileFacadeClass = programClassPool.getClass("TestKt")
        programClassPool.classesAccept(
            ReferencedKotlinMetadataVisitor(
                AllTypeAliasVisitor(allAnnotationVisitor)
            )
        )
        val annotation = slot<KotlinMetadataAnnotation>()

        "there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeAliasAnnotation(fileFacadeClass, ofType(KotlinTypeAliasMetadata::class), capture(annotation)) }
        }

        "the annotation class name should be correct" {
            annotation.captured.kmAnnotation.className shouldBe "MyTypeAliasAnnotation"
        }

        "the field values should be correct" {
            annotation.captured.kmAnnotation.arguments shouldContainExactly mapOf(
                "string" to StringValue("foo"),
                "byte" to ByteValue(1),
                "char" to CharValue('a'),
                "short" to ShortValue(1),
                "int" to IntValue(1),
                "long" to LongValue(1L),
                "float" to FloatValue(1f),
                "double" to DoubleValue(1.0),
                "boolean" to BooleanValue(true),
                "uByte" to UByteValue(1),
                "uShort" to UShortValue(1),
                "uInt" to UIntValue(1),
                "uLong" to ULongValue(1L),
                "kClass" to KClassValue("kotlin/String"),
                "enum" to EnumValue("MyEnum", "FOO"),
                "array" to ArrayValue(listOf(StringValue("foo"), StringValue("bar"))),
                "annotation" to AnnotationValue(KmAnnotation("Foo", mapOf("string" to StringValue("foo"))))
            )
        }
    }

    "Given a type with 1 annotation" - {
        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(ReferencedKotlinMetadataVisitor(allAnnotationVisitor))
        val annotation = slot<KotlinMetadataAnnotation>()

        "there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeAnnotation(fileFacadeClass, ofType(KotlinTypeMetadata::class), capture(annotation)) }
        }

        "the annotation class name should be correct" {
            annotation.captured.kmAnnotation.className shouldBe "MyTypeAnnotation"
        }
    }

    "Given a value parameter with 1 annotation" - {
        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(
            ReferencedKotlinMetadataVisitor(
                AllFunctionsVisitor(
                    KotlinFunctionFilter(
                        Predicate { func -> func.name == "foo" },
                        AllTypeParameterVisitor(
                            allAnnotationVisitor
                        )
                    )
                )
            )
        )
        val annotation = slot<KotlinMetadataAnnotation>()

        "there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeParameterAnnotation(fileFacadeClass, ofType(KotlinTypeParameterMetadata::class), capture(annotation)) }
        }

        "the annotation class name should be correct" {
            annotation.captured.kmAnnotation.className shouldBe "MyTypeParamAnnotation"
        }
    }
})
