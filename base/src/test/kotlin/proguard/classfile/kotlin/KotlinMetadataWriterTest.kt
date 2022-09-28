/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.classfile.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyAll
import kotlinx.metadata.internal.metadata.jvm.deserialization.JvmMetadataVersion
import proguard.classfile.MethodSignature
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
import proguard.classfile.kotlin.visitor.AllTypeVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeVisitor
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.classfile.util.kotlin.KotlinMetadataInitializer.MAX_SUPPORTED_VERSION
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.KotlinSource
import proguard.testutils.ReWritingMetadataVisitor

/**
 * Tests that the KotlinMetadataWriter correctly writes metadata to the
 * kotlin.Metadata annotation attached to classes.
 *
 * The tests here should use `ReWritingMetadataVisitor`: this wrapper
 * writes the current Kotlin metadata to the annotation, thus overwriting
 * the current metadata. It then re-initializes the metadata using `KotlinMetadataInitializer`.
 *
 * If the writer correctly wrote the metadata then the initializer should be able to
 * re-generate the model correctly.
 */
class KotlinMetadataWriterTest : FreeSpec({

    "Given a file facade" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                fun foo() {
                    println("foo")
                }

                class Foo

                typealias fooAlias = String

                val myFoo = "bar"
                """.trimIndent()
            )
        )

        val metadataVisitor = spyk<KotlinMetadataVisitor>()
        val functionVisitor = spyk<KotlinFunctionVisitor>()

        programClassPool.classesAccept(
            ReWritingMetadataVisitor(
                metadataVisitor,
                AllFunctionVisitor(functionVisitor)
            )
        )

        "Then the ownerClassName shouldBe correct" {
            verify(exactly = 1) {
                metadataVisitor.visitKotlinFileFacadeMetadata(
                    programClassPool.getClass("TestKt"),
                    withArg {
                        it.ownerClassName shouldBe "TestKt"
                    }
                )
            }
        }

        "Then there should be 1 function" {
            verify(exactly = 1) {
                functionVisitor.visitFunction(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFileFacadeKindMetadata>(),
                    withArg {
                        it.name shouldBe "foo"
                        it.jvmSignature shouldBe MethodSignature(null, "foo", "()V")
                    }
                )
            }
        }
    }

    "Given an annotation" - {
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

        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(
            ReWritingMetadataVisitor(
                AllKotlinAnnotationVisitor(annotationVisitor)
            )
        )
        val annotation = slot<KotlinAnnotation>()

        "Then there should be 1 annotation visited" {
            verify(exactly = 1) {
                annotationVisitor.visitTypeAnnotation(
                    fileFacadeClass,
                    ofType(KotlinTypeMetadata::class),
                    capture(annotation)
                )
            }
        }

        "Then the annotation argument values should be correctly set" {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReWritingMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor
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

    "Given an inline class" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                @JvmInline
                value class Password(private val s : String)
                """.trimIndent()
            )
        )

        val clazz = programClassPool.getClass("Password")
        val kotlinTypeVisitor = spyk<KotlinTypeVisitor>()

        clazz.accept(
            ReWritingMetadataVisitor(
                AllTypeVisitor(kotlinTypeVisitor)
            )
        )

        "Then the type and name should be correct" {
            verify {
                kotlinTypeVisitor.visitInlineClassUnderlyingPropertyType(
                    clazz,
                    withArg {
                        it.underlyingPropertyName shouldBe "s"
                        it.underlyingPropertyType.className shouldBe "kotlin/String"
                    },
                    ofType<KotlinTypeMetadata>()
                )
            }
        }
    }

    "Given a Kotlin class with a incompatible metadata version" - {
        val unsupportedVersion = KotlinMetadataVersion(1, 3, 0)

        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestCompatibleMetadata.java",
                """
                        @kotlin.Metadata(
                            d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                            d2 = {"LTestCompatibleMetadata;", "", "()V"},
                            k = 1,
                            mv = {${unsupportedVersion.major}, ${unsupportedVersion.minor}, ${unsupportedVersion.patch}}
                        )
                        public class TestCompatibleMetadata { }
                """.trimIndent()
            )
        )
        programClassPool.classesAccept(KotlinMetadataInitializer { _, _ -> })
        val clazz = programClassPool.getClass("TestCompatibleMetadata")

        "Then the compatible version from the metadata library should be written" {
            val visitor = spyk<KotlinMetadataVisitor>()

            clazz.accept(ReWritingMetadataVisitor(visitor))

            verify {
                visitor.visitKotlinClassMetadata(
                    clazz,
                    withArg {
                        it.mv shouldBe JvmMetadataVersion.INSTANCE.toArray()
                    }
                )
            }
        }
    }

    "Given a Kotlin class with a compatible metadata version" - {
        val maxVersion = MAX_SUPPORTED_VERSION

        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "TestCompatibleMetadata.java",
                """
                        @kotlin.Metadata(
                            d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002"},
                            d2 = {"LTestCompatibleMetadata;", "", "()V"},
                            k = 1,
                            mv = {${maxVersion.major}, ${maxVersion.minor}, ${maxVersion.patch}}
                        )
                        public class TestCompatibleMetadata { }
                """.trimIndent()
            )
        )
        programClassPool.classesAccept(KotlinMetadataInitializer { _, _ -> })
        val clazz = programClassPool.getClass("TestCompatibleMetadata")

        "Then the compatible version from the metadata library should be written" {
            val visitor = spyk<KotlinMetadataVisitor>()

            clazz.accept(ReWritingMetadataVisitor(visitor))

            verify {
                visitor.visitKotlinClassMetadata(
                    clazz,
                    withArg {
                        it.mv shouldBe maxVersion.toArray()
                    }
                )
            }
        }
    }
})
