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

import io.kotest.assertions.shouldFail
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.Clazz
import proguard.classfile.attribute.annotation.visitor.AllAnnotationVisitor
import proguard.classfile.attribute.annotation.visitor.AnnotationTypeFilter
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.io.kotlin.KotlinMetadataWriter
import proguard.classfile.kotlin.visitor.AllFunctionsVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllKotlinPropertiesVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinFunctionVisitor
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinTypeAliasVisitor
import proguard.classfile.kotlin.visitor.MultiKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinClassKindFilter
import proguard.classfile.util.WarningPrinter
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MultiClassVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource
import java.io.OutputStream
import java.io.PrintWriter

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
        val programClassPool = ClassPoolBuilder.fromSource(
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
        val classVisitor = spyk<KotlinMetadataVisitor>()
        val functionVisitor = spyk<KotlinFunctionVisitor>()
        val typeAliasVisitor = spyk<KotlinTypeAliasVisitor>()
        val propertyVisitor = spyk<KotlinPropertyVisitor>()

        programClassPool.classesAccept(
            ReWritingMetadataVisitor(
                metadataVisitor,
                AllFunctionsVisitor(functionVisitor),
                KotlinClassKindFilter(classVisitor),
                AllTypeAliasVisitor(typeAliasVisitor),
                AllKotlinPropertiesVisitor(propertyVisitor)
            )
        )

        "Then the ownerClassName shouldBe correct" {
            shouldFail {
                // TODO(T5348): this currently fails because the ownerClassName
                //              should be set in the initializer not only in ClassReferenceInitializer

                verify(exactly = 1) {
                    metadataVisitor.visitKotlinFileFacadeMetadata(
                        programClassPool.getClass("TestKt"),
                        withArg {
                            it.ownerClassName shouldBe "TestKt"
                        }
                    )
                }
            }
        }

        "Then there should be 1 function" {
            verify(exactly = 1) {
                functionVisitor.visitFunction(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFileFacadeKindMetadata>(),
                    withArg {
                        it.name shouldBe "foo"
                        it.jvmSignature shouldBe JvmMethodSignature("foo", "()V")
                    }
                )
            }
        }

        "Then there should be 1 class" {
            verify(exactly = 1) {
                classVisitor.visitKotlinClassMetadata(
                    programClassPool.getClass("Foo"),
                    withArg {
                        it.className shouldBe "Foo"
                    }
                )
            }
        }

        "Then there should be 1 type alias" {
            verify(exactly = 1) {
                typeAliasVisitor.visitTypeAlias(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFileFacadeKindMetadata>(),
                    withArg {
                        it.name shouldBe "fooAlias"
                    }
                )
            }
        }

        "Then there should be 1 property" {
            verify(exactly = 1) {
                propertyVisitor.visitProperty(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinFileFacadeKindMetadata>(),
                    withArg {
                        it.name shouldBe "myFoo"
                    }
                )
            }
        }
    }

    "Given an annotation" - {
        val programClassPool = ClassPoolBuilder.fromSource(
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
        val annotation = slot<KotlinMetadataAnnotation>()

        "Then there should be 1 annotation visited" {
            verify(exactly = 1) {
                annotationVisitor.visitTypeAnnotation(
                    fileFacadeClass,
                    ofType(KotlinTypeMetadata::class),
                    capture(annotation)
                )
            }
        }

        // TODO(T2698): Add further annotation tests when the new model is ready
    }
})

internal class ReWritingMetadataVisitor(private vararg val visitors: KotlinMetadataVisitor) : ClassVisitor {
    private val warningPrinter = WarningPrinter(
        PrintWriter(object : OutputStream() {
            // TODO: when switching to Java 11, we can use `OutputStream.nullOutputStream()`
            override fun write(b: Int) { }
        })
    )

    override fun visitAnyClass(clazz: Clazz?) {

        clazz?.accept(
            MultiClassVisitor(
                ReferencedKotlinMetadataVisitor(
                    KotlinMetadataWriter(warningPrinter)
                ),
                AllAttributeVisitor(
                    AllAnnotationVisitor(
                        AnnotationTypeFilter(
                            KotlinConstants.TYPE_KOTLIN_METADATA,
                            KotlinMetadataInitializer(warningPrinter)
                        )
                    )
                ),
                ReferencedKotlinMetadataVisitor(
                    MultiKotlinMetadataVisitor(*visitors)
                )
            )
        )
    }
}
