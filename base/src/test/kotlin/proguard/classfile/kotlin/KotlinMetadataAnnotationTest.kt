/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyAll
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.kotlin.KotlinAnnotationArgument.ArrayValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.BooleanValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ByteValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.CharValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ClassValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.DoubleValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.EnumValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.FloatValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.IntValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.LongValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ShortValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.StringValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.UByteValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.UIntValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.ULongValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.UShortValue
import proguard.classfile.kotlin.visitor.AllFunctionVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.AllTypeParameterVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter
import proguard.classfile.visitor.ClassVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import java.util.function.Predicate

class KotlinMetadataAnnotationTest : FreeSpec({

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
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
        val fileFacadeClass = programClassPool.getClass("TestKt")

        "Then there should be 1 annotation visited" {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllTypeAliasVisitor(allAnnotationVisitor)
                )
            )

            verify(exactly = 1) {
                annotationVisitor.visitTypeAliasAnnotation(
                    fileFacadeClass,
                    ofType(KotlinTypeAliasMetadata::class),
                    withArg {
                        it.className shouldBe "MyTypeAliasAnnotation"
                    }
                )
            }
        }

        "Then the referenced class should be visitable" {
            val classVisitor = spyk<ClassVisitor>()
            val annotationVisitor = KotlinAnnotationVisitor { _, _, annotation ->
                annotation.referencedClassAccept(classVisitor)
            }
            val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllTypeAliasVisitor(allAnnotationVisitor)
                )
            )

            verify(exactly = 1) {
                classVisitor.visitProgramClass(
                    programClassPool.getClass("MyTypeAliasAnnotation") as ProgramClass
                )
            }
        }

        "Then the argument values should be correct" {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllTypeAliasVisitor(
                        AllKotlinAnnotationVisitor(
                            AllKotlinAnnotationArgumentVisitor(annotationArgVisitor)
                        )
                    )
                )
            )

            verifyAll {
                annotationArgVisitor.visitAnyArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    ofType<KotlinAnnotationArgument>(),
                    ofType<KotlinAnnotationArgument.Value>()
                )

                annotationArgVisitor.visitAnyLiteralArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    ofType<KotlinAnnotationArgument>(),
                    ofType<KotlinAnnotationArgument.LiteralValue<*>>()
                )

                annotationArgVisitor.visitStringArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "string" },
                    StringValue("foo")
                )

                annotationArgVisitor.visitByteArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "byte" },
                    ByteValue(1)
                )

                annotationArgVisitor.visitCharArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "char" },
                    CharValue('a')
                )

                annotationArgVisitor.visitShortArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "short" },
                    ShortValue(1)
                )

                annotationArgVisitor.visitIntArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "int" },
                    IntValue(1)
                )

                annotationArgVisitor.visitLongArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "long" },
                    LongValue(1L)
                )

                annotationArgVisitor.visitFloatArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "float" },
                    FloatValue(1f)
                )

                annotationArgVisitor.visitDoubleArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "double" },
                    DoubleValue(1.0)
                )

                annotationArgVisitor.visitBooleanArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "boolean" },
                    BooleanValue(true)
                )

                annotationArgVisitor.visitUByteArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "uByte" },
                    UByteValue(1)
                )

                annotationArgVisitor.visitUShortArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "uShort" },
                    UShortValue(1)
                )

                annotationArgVisitor.visitUIntArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "uInt" },
                    UIntValue(1)
                )

                annotationArgVisitor.visitULongArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "uLong" },
                    ULongValue(1)
                )

                annotationArgVisitor.visitEnumArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "enum" },
                    EnumValue("MyEnum", "FOO")
                )

                annotationArgVisitor.visitArrayArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "array" },
                    ArrayValue(listOf(StringValue("foo"), StringValue("bar")))
                )

                annotationArgVisitor.visitAnnotationArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "annotation" },
                    withArg {
                        it.kotlinMetadataAnnotation.className shouldBe "Foo"
                    }
                )

                annotationArgVisitor.visitClassArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { it.name shouldBe "kClass" },
                    ClassValue("kotlin/String")
                )
            }
        }
    }

    "Given a type with 1 annotation" - {
        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(ReferencedKotlinMetadataVisitor(allAnnotationVisitor))
        val annotation = slot<KotlinAnnotation>()

        "Then there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeAnnotation(fileFacadeClass, ofType(KotlinTypeMetadata::class), capture(annotation)) }
        }

        "Then the annotation class name should be correct" {
            annotation.captured.className shouldBe "MyTypeAnnotation"
        }
    }

    "Given a value parameter with 1 annotation" - {
        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(
            ReferencedKotlinMetadataVisitor(
                AllFunctionVisitor(
                    KotlinFunctionFilter(
                        Predicate { func -> func.name == "foo" },
                        AllTypeParameterVisitor(
                            allAnnotationVisitor
                        )
                    )
                )
            )
        )
        val annotation = slot<KotlinAnnotation>()

        "Then there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeParameterAnnotation(fileFacadeClass, ofType(KotlinTypeParameterMetadata::class), capture(annotation)) }
        }

        "Then the annotation class name should be correct" {
            annotation.captured.className shouldBe "MyTypeParamAnnotation"
        }
    }

    "Given an annotation without the referenced class initialized" - {
        val annotation = KotlinAnnotation("A")

        "Then the referenced class should not be visited" {
            val classVisitor = spyk<ClassVisitor>()
            annotation.referencedClassAccept(classVisitor)
            verify(exactly = 0) { classVisitor.visitAnyClass(ofType<Clazz>()) }
        }
    }

    // Basic equality tests

    "Given an annotation" - {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo")))
        )

        "Then the toString should printed a string representation" {
            annotation1.toString() shouldBe "A(arg1 = foo)"
        }

        "Then it should not be equal to null" {
            annotation1.equals(null) shouldBe false
        }

        "Then it should not be equal to a different object" {
            (annotation1.equals("String")) shouldBe false
        }

        "Then it should be equal to itself" {
            (annotation1.equals(annotation1)) shouldBe true
        }
    }

    "Given 2 annotations with the same name" - {
        val annotation1 = KotlinAnnotation("A")
        val annotation2 = KotlinAnnotation("A")

        "Then they should be equal" {
            annotation1 shouldBe annotation2
        }

        "Then they should have the same hashCode" {
            annotation1.hashCode() shouldBe annotation2.hashCode()
        }
    }

    "Given 2 annotations with the same name and arguments" - {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo")))
        )

        val annotation2 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo")))
        )

        "Then they should be equal" {
            annotation1 shouldBe annotation2
        }

        "Then they should have the same hashCode" {
            annotation1.hashCode() shouldBe annotation2.hashCode()
        }
    }

    "Given 2 annotations with the same name and different arguments" - {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo")))
        )

        val annotation2 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("bar")))
        )

        "Then they should not be equal" {
            annotation1 shouldNotBe annotation2
        }

        "Then they should not have the same hashCode" {
            annotation1.hashCode() shouldNotBe annotation2.hashCode()
        }
    }

    "Given 2 annotations with different names" - {
        val annotation1 = KotlinAnnotation("A")
        val annotation2 = KotlinAnnotation("B")

        "Then they should not be equal" {
            annotation1 shouldNotBe annotation2
        }

        "Then they should not have the same hashCode" {
            annotation1.hashCode() shouldNotBe annotation2.hashCode()
        }
    }

    "Given 2 annotations with different name and arguments" - {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo")))
        )

        val annotation2 = KotlinAnnotation(
            "B",
            listOf(KotlinAnnotationArgument("arg1", StringValue("bar")))
        )

        "Then they should not be equal" {
            annotation1 shouldNotBe annotation2
        }

        "Then they should not have the same hashCode" {
            annotation1.hashCode() shouldNotBe annotation2.hashCode()
        }
    }
})
