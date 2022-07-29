/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.util

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.ClassPool
import proguard.classfile.Method
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.kotlin.KotlinAnnotatable
import proguard.classfile.kotlin.KotlinAnnotation
import proguard.classfile.kotlin.KotlinAnnotationArgument
import proguard.classfile.kotlin.KotlinAnnotationArgument.StringValue
import proguard.classfile.kotlin.KotlinAnnotationArgument.Value
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata
import proguard.classfile.kotlin.KotlinTypeMetadata
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllPropertyVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinPropertyVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.kotlin.visitor.filter.KotlinAnnotationArgumentFilter
import proguard.classfile.kotlin.visitor.filter.KotlinPropertyFilter
import proguard.classfile.util.kotlin.KotlinMetadataInitializer
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.MemberNameFilter
import proguard.classfile.visitor.MemberVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource
import java.lang.RuntimeException
import java.util.function.Predicate
import proguard.classfile.kotlin.KotlinConstants.dummyClassPool as kotlinDummyClassPool

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
                    val array: Array<Foo>,
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
                    array = arrayOf(Foo("foo"), Foo("bar")),
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

        "Then there should be 1 annotation visited" {
            verify(exactly = 1) {
                annotationVisitor.visitTypeAnnotation(fileFacadeClass, ofType(KotlinTypeMetadata::class), capture(annotation))
            }
        }

        "Then the annotation referenced class should be correct" {
            annotation.captured.className shouldBe "MyTypeAnnotation"
            annotation.captured.referencedAnnotationClass shouldBe programClassPool.getClass("MyTypeAnnotation")
        }

        "Then the annotation argument value references should be correctly set" {

            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor
                        )
                    )
                )
            )

            verify(exactly = 17) {
                annotationArgVisitor.visitAnyArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    withArg { argument ->
                        with(programClassPool.getClass("MyTypeAnnotation")) {
                            when (argument.name) {
                                "string" -> argument.referencedAnnotationMethod shouldBe findMethod("string", "()Ljava/lang/String;")
                                "byte" -> argument.referencedAnnotationMethod shouldBe findMethod("byte", "()B")
                                "char" -> argument.referencedAnnotationMethod shouldBe findMethod("char", "()C")
                                "short" -> argument.referencedAnnotationMethod shouldBe findMethod("short", "()S")
                                "int" -> argument.referencedAnnotationMethod shouldBe findMethod("int", "()I")
                                "long" -> argument.referencedAnnotationMethod shouldBe findMethod("long", "()J")
                                "float" -> argument.referencedAnnotationMethod shouldBe findMethod("float", "()F")
                                "double" -> argument.referencedAnnotationMethod shouldBe findMethod("double", "()D")
                                "boolean" -> argument.referencedAnnotationMethod shouldBe findMethod("boolean", "()Z")
                                "uByte" -> argument.referencedAnnotationMethod shouldBe findMethod("uByte", "()B")
                                "uShort" -> argument.referencedAnnotationMethod shouldBe findMethod("uShort", "()S")
                                "uInt" -> argument.referencedAnnotationMethod shouldBe findMethod("uInt", "()I")
                                "uLong" -> argument.referencedAnnotationMethod shouldBe findMethod("uLong", "()J")
                                "enum" -> argument.referencedAnnotationMethod shouldBe findMethod("enum", "()LMyEnum;")
                                "array" -> argument.referencedAnnotationMethod shouldBe findMethod("array", "()[LFoo;")
                                "annotation" -> argument.referencedAnnotationMethod shouldBe findMethod("annotation", "()LFoo;")
                                "kClass" -> argument.referencedAnnotationMethod shouldBe findMethod("kClass", "()Ljava/lang/Class;")
                                else -> RuntimeException("Unexpected argument $argument")
                            }
                        }
                    },
                    ofType<Value>()
                )
            }
        }

        "Then the class argument values should have their references correctly set" {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            KotlinAnnotationArgumentFilter(
                                Predicate<KotlinAnnotationArgument> { it.name == "kClass" },
                                annotationArgVisitor
                            )
                        )
                    )
                )
            )

            verify(exactly = 1) {
                annotationArgVisitor.visitClassArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    ofType<KotlinAnnotationArgument>(),
                    withArg {
                        it.className shouldBe "kotlin/String"
                        it.referencedClass shouldBe kotlinDummyClassPool.getClass("kotlin/String")
                    }
                )
            }
        }

        "Then the enum argument values should have their references correctly set" {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            KotlinAnnotationArgumentFilter(
                                Predicate<KotlinAnnotationArgument> { it.name == "enum" },
                                annotationArgVisitor
                            )
                        )
                    )
                )
            )

            verify(exactly = 1) {
                annotationArgVisitor.visitEnumArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    ofType<KotlinAnnotationArgument>(),
                    withArg {
                        it.className shouldBe "MyEnum"
                        it.referencedClass shouldBe programClassPool.getClass("MyEnum")
                    }
                )
            }
        }

        "Then the annotation argument values should have their references correctly set" {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            KotlinAnnotationArgumentFilter(
                                Predicate<KotlinAnnotationArgument> { it.name == "annotation" },
                                annotationArgVisitor
                            )
                        )
                    )
                )
            )

            verify(exactly = 1) {
                annotationArgVisitor.visitAnnotationArgument(
                    programClassPool.getClass("TestKt"),
                    ofType<KotlinAnnotatable>(),
                    ofType<KotlinAnnotation>(),
                    ofType<KotlinAnnotationArgument>(),
                    withArg {
                        it.kotlinMetadataAnnotation shouldBe
                            KotlinAnnotation(
                                "Foo",
                                listOf(KotlinAnnotationArgument("string", StringValue("foo")))
                            )
                    }
                )
            }
        }
    }

    "Given a Kotlin function with named types" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                // This snippet generates types annotated with `kotlin.ParameterName`:
                // the `ClassReferenceInitializer` should not crash when it cannot find
                // library classes.
                fun foo(p: (x: Int, y: Int) -> Unit) = p(1, 2)
                """.trimIndent()
            )
        )

        "Then an exception should not be thrown when library classes are missing" {
            shouldNotThrow<Exception> {
                programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, ClassPool()))
            }
        }
    }

    "Given a companion object with a constant property" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                class BaseClass {
                    companion object {
                        val CONSTANT = "This does not need the base class"
                    }
                }
                """.trimIndent()
            ),
            initialize = false
        )

        "Then an exception should not be thrown if the base class is missing" {
            programClassPool.removeClass("BaseClass")
            programClassPool.classesAccept(
                KotlinMetadataInitializer { _, message ->
                    println(
                        message
                    )
                }
            )
            shouldNotThrow<Exception> {
                programClassPool.classesAccept(
                    ClassReferenceInitializer(programClassPool, ClassPool())
                )
            }
        }
    }

    "Given an annotation class containing annotations" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "MyAnnotation.kt",
                """
                   annotation class MyAnnotation(
                        // The annotation will be stored on a method `foo${'$'}annotation` in `MyAnnotation${'$'}DefaultImpls`
                        @Deprecated("Use bar instead")
                        val foo: String = "foo",
                        val bar: String = "bar"
                   )
                """.trimIndent()
            )
        )

        "Then the annotation synthetic method should be initialized correctly" {
            val visitor = spyk<KotlinPropertyVisitor>()
            programClassPool.classesAccept(
                "MyAnnotation",
                ReferencedKotlinMetadataVisitor(
                    AllPropertyVisitor(
                        KotlinPropertyFilter({ it.name == "foo" }, visitor)
                    )
                )
            )

            lateinit var syntheticAnnotationMethod: Method

            programClassPool.classesAccept(
                "MyAnnotation\$DefaultImpls",
                AllMemberVisitor(
                    MemberNameFilter(
                        "foo\$annotations",
                        object : MemberVisitor {
                            override fun visitProgramMethod(programClass: ProgramClass, programMethod: ProgramMethod) {
                                syntheticAnnotationMethod = programMethod
                            }
                        }
                    )
                )
            )

            syntheticAnnotationMethod shouldNotBe null

            verify(exactly = 1) {
                visitor.visitAnyProperty(
                    programClassPool.getClass("MyAnnotation"),
                    ofType<KotlinDeclarationContainerMetadata>(),
                    withArg {
                        it.syntheticMethodForAnnotations shouldBe MethodSignature(null, "foo\$annotations", "()V")
                        it.referencedSyntheticMethodForAnnotations shouldBe syntheticAnnotationMethod
                    }
                )
            }
        }
    }
})
