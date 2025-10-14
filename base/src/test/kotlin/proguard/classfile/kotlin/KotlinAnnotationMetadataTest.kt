package proguard.classfile.kotlin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
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
import proguard.classfile.kotlin.flags.KotlinPropertyAccessorMetadata
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.AllTypeAliasVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationArgumentVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.visitor.ClassVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.KotlinSource

class KotlinAnnotationMetadataTest : BehaviorSpec({
    Given("a type alias with 1 annotation") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
            import kotlin.reflect.KClass

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

            // extra helpers

            enum class MyEnum { FOO, BAR }
            annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        When("visiting all annotations and their arguments") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val fileFacadeClass = programClassPool.getClass("TestKt")

            fileFacadeClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
            )

            val annotation = slot<KotlinAnnotation>()

            Then("the type alias annotation should be visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitTypeAliasAnnotation(
                        fileFacadeClass,
                        ofType(KotlinTypeAliasMetadata::class),
                        capture(annotation),
                    )
                }
            }

            Then("the annotation argument values should be correctly set") {

                verifyAnnotationArguments(
                    annotationArgVisitor,
                    fileFacadeClass,
                    annotation.captured,
                )
            }

            Then("the referenced class should be visitable") {
                val classVisitor = spyk<ClassVisitor>()
                val annotationVisitor = KotlinAnnotationVisitor { _, _, annotation ->
                    annotation.referencedClassAccept(classVisitor)
                }
                val allAnnotationVisitor = AllKotlinAnnotationVisitor(annotationVisitor)

                programClassPool.classesAccept(
                    ReferencedKotlinMetadataVisitor(
                        AllTypeAliasVisitor(allAnnotationVisitor),
                    ),
                )

                verify(exactly = 1) {
                    classVisitor.visitProgramClass(
                        programClassPool.getClass("MyTypeAliasAnnotation") as ProgramClass,
                    )
                }
            }
        }
    }

    Given("an annotation without the referenced class initialized") {
        val annotation = KotlinAnnotation("A")

        Then("the referenced class should not be visited") {
            val classVisitor = spyk<ClassVisitor>()
            annotation.referencedClassAccept(classVisitor)
            verify(exactly = 0) { classVisitor.visitAnyClass(ofType<Clazz>()) }
        }
    }

    Given("an annotation") {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo"))),
        )

        Then("the toString should printed a string representation") {
            annotation1.toString() shouldBe "A(arg1 = foo)"
        }

        Then("it should not be equal to null") {
            annotation1.equals(null) shouldBe false
        }

        Then("it should not be equal to a different object") {
            (annotation1.equals("String")) shouldBe false
        }

        Then("it should be equal to itself") {
            (annotation1.equals(annotation1)) shouldBe true
        }
    }

    Given("2 annotations with the same name") {
        val annotation1 = KotlinAnnotation("A")
        val annotation2 = KotlinAnnotation("A")

        Then("they should be equal") {
            annotation1 shouldBe annotation2
        }

        Then("they should have the same hashCode") {
            annotation1.hashCode() shouldBe annotation2.hashCode()
        }
    }

    Given("2 annotations with the same name and arguments") {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo"))),
        )

        val annotation2 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo"))),
        )

        Then("they should be equal") {
            annotation1 shouldBe annotation2
        }

        Then("they should have the same hashCode") {
            annotation1.hashCode() shouldBe annotation2.hashCode()
        }
    }

    Given("annotations with the same name and different arguments") {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo"))),
        )

        val annotation2 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("bar"))),
        )

        Then("they should not be equal") {
            annotation1 shouldNotBe annotation2
        }

        Then("they should not have the same hashCode") {
            annotation1.hashCode() shouldNotBe annotation2.hashCode()
        }
    }

    Given("annotations with different names") {
        val annotation1 = KotlinAnnotation("A")
        val annotation2 = KotlinAnnotation("B")

        Then("they should not be equal") {
            annotation1 shouldNotBe annotation2
        }

        Then("they should not have the same hashCode") {
            annotation1.hashCode() shouldNotBe annotation2.hashCode()
        }
    }

    Given("annotations with different name and arguments") {
        val annotation1 = KotlinAnnotation(
            "A",
            listOf(KotlinAnnotationArgument("arg1", StringValue("foo"))),
        )

        val annotation2 = KotlinAnnotation(
            "B",
            listOf(KotlinAnnotationArgument("arg1", StringValue("bar"))),
        )

        Then("they should not be equal") {
            annotation1 shouldNotBe annotation2
        }

        Then("they should not have the same hashCode") {
            annotation1.hashCode() shouldNotBe annotation2.hashCode()
        }
    }

    Given("An annotated type") {
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
                    array = ["foo", "bar"],
                    annotation = Foo("foo")) String = "foo"

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
        )

        When("visiting all annotations and their arguments") {

            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val fileFacadeClass = programClassPool.getClass("TestKt")

            fileFacadeClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
            )

            val annotation = slot<KotlinAnnotation>()

            Then("the type annotation should be visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitTypeAnnotation(
                        fileFacadeClass,
                        ofType(KotlinTypeMetadata::class),
                        capture(annotation),
                    )
                }
            }

            Then("the annotation argument values should be correctly set") {

                verifyAnnotationArguments(
                    annotationArgVisitor,
                    fileFacadeClass,
                    annotation.captured,
                )
            }
        }
    }

    Given("An annotated function") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.FUNCTION)
                annotation class MyFunctionAnnotation(
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

                @MyFunctionAnnotation(
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
                    array = ["foo", "bar"],
                    annotation = Foo("foo")) fun foo(): String { return "Foo" }

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        When("visiting all annotations and their arguments") {

            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val fileFacadeClass = programClassPool.getClass("TestKt")

            fileFacadeClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
            )

            val annotation = slot<KotlinAnnotation>()

            Then("the function's annotation should be visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitFunctionAnnotation(
                        fileFacadeClass,
                        ofType(KotlinFunctionMetadata::class),
                        capture(annotation),
                    )
                }
            }

            Then("the annotation argument values should be correctly set") {
                verifyAnnotationArguments(
                    annotationArgVisitor,
                    fileFacadeClass,
                    annotation.captured,
                )
            }
        }
    }

    Given("Annotated value parameters") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.VALUE_PARAMETER)
                annotation class MyValueParameterAnnotation(
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
                
                fun foo(
                    @MyValueParameterAnnotation(
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
                        array = ["foo", "bar"],
                        annotation = Foo("foo"))
                        valueParam : String = "default" ): String { return valueParam }

                class Bar(
                    @MyValueParameterAnnotation(
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
                        array = ["foo", "bar"],
                        annotation = Foo("foo"))
                        valueParam : String = "default")
                        
                var myProperty = "initialized"
                    set(
                        @MyValueParameterAnnotation(
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
                        array = ["foo", "bar"],
                        annotation = Foo("foo"))
                        valueParam) { field = valueParam }

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        When("visiting all annotations and their arguments") {

            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val fileFacadeAnnotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()
            val barAnnotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val fileFacadeClass = programClassPool.getClass("TestKt")
            val barClass = programClassPool.getClass("Bar")

            fileFacadeClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            fileFacadeAnnotationArgVisitor,
                        ),
                    ),
                ),
            )
            barClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            barAnnotationArgVisitor,
                        ),
                    ),
                ),
            )

            val barAnnotation = slot<KotlinAnnotation>()
            val fileFacadeAnnotations = mutableListOf<KotlinAnnotation>()

            Then("there should be 2 annotations visited on the file facade class") {
                verify(exactly = 2) {
                    annotationVisitor.visitValueParameterAnnotation(
                        fileFacadeClass,
                        ofType(KotlinValueParameterMetadata::class),
                        capture(fileFacadeAnnotations),
                    )
                }
            }

            Then("there should be 1 annotation visited on the Bar class") {
                verify(exactly = 1) {
                    annotationVisitor.visitValueParameterAnnotation(
                        barClass,
                        ofType(KotlinValueParameterMetadata::class),
                        capture(barAnnotation),
                    )
                }
            }

            Then("the annotation argument values should be correctly set on the function and property value parameters") {
                verifyAnnotationArguments(fileFacadeAnnotationArgVisitor, fileFacadeClass, fileFacadeAnnotations[0])
                verifyAnnotationArguments(fileFacadeAnnotationArgVisitor, fileFacadeClass, fileFacadeAnnotations[1])
                verifyAnnotationArguments(barAnnotationArgVisitor, barClass, barAnnotation.captured)
            }
        }
    }

    Given("An annotated property with annotated accessor methods") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.PROPERTY,AnnotationTarget.PROPERTY_GETTER,AnnotationTarget.PROPERTY_SETTER)
                annotation class MyPropertyAnnotation(
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

                @all:MyPropertyAnnotation(
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
                array = ["foo", "bar"],
                annotation = Foo("foo"))
                var myProperty = "initialized"
                    get() = "Get"
                    @MyPropertyAnnotation(
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
                                    array = ["foo", "bar"],
                                    annotation = Foo("foo"))
                    set(value) { field = value}

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata", "-Xannotation-target-all"),
        )

        When("visiting all annotations and their arguments") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val fileFacadeClass = programClassPool.getClass("TestKt")

            val annotations = mutableListOf<KotlinAnnotation>()

            fileFacadeClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
            )

            Then("the property annotation is visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitPropertyAnnotation(
                        fileFacadeClass,
                        withArg { it.name shouldBe "myProperty" },
                        ofType<KotlinAnnotation>(),
                    )
                }
            }

            Then("both getter and setter annotation have correct arguments") {

                verify(exactly = 2) {
                    annotationVisitor.visitPropertyAccessorAnnotation(
                        fileFacadeClass,
                        ofType(KotlinPropertyAccessorMetadata::class),
                        capture(annotations),
                    )
                }

                verifyAnnotationArguments(
                    annotationArgVisitor,
                    fileFacadeClass,
                    annotations[0],
                )

                verifyAnnotationArguments(
                    annotationArgVisitor,
                    fileFacadeClass,
                    annotations[1],
                )
            }
        }
    }

    Given("An annotated class with an annotated constructor") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.CLASS)
                annotation class MyClassAnnotation(
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

                @Target(AnnotationTarget.CONSTRUCTOR)
                annotation class MyConstructorAnnotation(
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

                @MyClassAnnotation(
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
                    array = ["foo", "bar"],
                    annotation = Foo("foo")) 
                    class MyAnnotatedClass 
                    @MyConstructorAnnotation(
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
                    array = ["foo", "bar"],
                    annotation = Foo("foo")) constructor(val myAnnotatedConstructor : String)

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        When("visiting all annotations and their arguments") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val annotatedClass = programClassPool.getClass("MyAnnotatedClass")

            val classAnnotation = slot<KotlinAnnotation>()
            val constructorAnnotation = slot<KotlinAnnotation>()
            annotatedClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
            )

            Then("there should be 1 class annotation visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitClassAnnotation(
                        annotatedClass,
                        ofType(KotlinClassKindMetadata::class),
                        capture(classAnnotation),
                    )
                }
            }

            Then("there should be 1 constructor annotation visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitConstructorAnnotation(
                        annotatedClass,
                        ofType(KotlinConstructorMetadata::class),
                        capture(constructorAnnotation),
                    )
                }
            }

            Then("the class & constructor annotation argument values should be correctly set") {
                verifyAnnotationArguments(
                    annotationArgVisitor,
                    annotatedClass,
                    classAnnotation.captured,
                )
                verifyAnnotationArguments(
                    annotationArgVisitor,
                    annotatedClass,
                    constructorAnnotation.captured,
                )
            }
        }
    }

    Given("An annotated enum value") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.FIELD)
                annotation class MyEnumEntryAnnotation(
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

                enum class MyEnumClass {
                @MyEnumEntryAnnotation(
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
                    array = ["foo", "bar"],
                    annotation = Foo("foo")) MY_ENUM_ENTRY
                }

                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        When("visiting all annotations and their arguments") {
            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val myEnumClass = programClassPool.getClass("MyEnumClass")

            val enumEntryAnnotation = slot<KotlinAnnotation>()

            myEnumClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
            )

            Then("there should be 1 enum entry annotation visited") {
                verify(exactly = 1) {
                    annotationVisitor.visitEnumEntryAnnotation(
                        myEnumClass,
                        ofType(KotlinEnumEntryMetadata::class),
                        capture(enumEntryAnnotation),
                    )
                }
            }

            Then("the enum entry annotation argument values should be correctly set") {
                verifyAnnotationArguments(
                    annotationArgVisitor,
                    myEnumClass,
                    enumEntryAnnotation.captured,
                )
            }
        }
    }

    Given("An annotated value parameter types of a constructor") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                import kotlin.reflect.KClass

                @Target(AnnotationTarget.TYPE)
                annotation class MyValueParameterTypeAnnotation(
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
                

                class Bar(
                    valueParam :
                    @MyValueParameterTypeAnnotation(
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
                        array = ["foo", "bar"],
                        annotation = Foo("foo"))
                    String = "default")
                    
                // extra helpers

                enum class MyEnum { FOO, BAR }
                annotation class Foo(val string: String)
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xannotations-in-metadata"),
        )

        When("visiting all annotations and their arguments") {

            val annotationVisitor = spyk<KotlinAnnotationVisitor>()
            val barAnnotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            val barClass = programClassPool.getClass("Bar")

            barClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(annotationVisitor),
                ),
            )

            barClass.accept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            barAnnotationArgVisitor,
                        ),
                    ),
                ),
            )

            val barAnnotation = slot<KotlinAnnotation>()

            Then("there should be 1 annotation visited on the Bar class") {
                verify(exactly = 1) {
                    annotationVisitor.visitAnyAnnotation(
                        barClass,
                        ofType(KotlinTypeMetadata::class),
                        capture(barAnnotation),
                    )
                }
            }

            Then("the annotation argument values should be correctly set") {
                verifyAnnotationArguments(barAnnotationArgVisitor, barClass, barAnnotation.captured)
            }
        }
    }
})

private fun verifyAnnotationArguments(
    annotationArgVisitor: KotlinAnnotationArgumentVisitor,
    clazz: Clazz,
    annotation: KotlinAnnotation,
) {
    verify {
        annotationArgVisitor.visitAnyArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            ofType<KotlinAnnotationArgument>(),
            ofType<KotlinAnnotationArgument.Value>(),
        )

        annotationArgVisitor.visitAnyLiteralArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            ofType<KotlinAnnotationArgument>(),
            ofType<KotlinAnnotationArgument.LiteralValue<*>>(),
        )

        annotationArgVisitor.visitStringArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "string" },
            StringValue("foo"),
        )

        annotationArgVisitor.visitByteArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "byte" },
            ByteValue(1),
        )

        annotationArgVisitor.visitCharArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "char" },
            CharValue('a'),
        )

        annotationArgVisitor.visitShortArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "short" },
            ShortValue(1),
        )

        annotationArgVisitor.visitIntArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "int" },
            IntValue(1),
        )

        annotationArgVisitor.visitLongArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "long" },
            LongValue(1L),
        )

        annotationArgVisitor.visitFloatArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "float" },
            FloatValue(1f),
        )

        annotationArgVisitor.visitDoubleArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "double" },
            DoubleValue(1.0),
        )

        annotationArgVisitor.visitBooleanArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "boolean" },
            BooleanValue(true),
        )

        annotationArgVisitor.visitUByteArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "uByte" },
            UByteValue(1),
        )

        annotationArgVisitor.visitUShortArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "uShort" },
            UShortValue(1),
        )

        annotationArgVisitor.visitUIntArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "uInt" },
            UIntValue(1),
        )

        annotationArgVisitor.visitULongArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "uLong" },
            ULongValue(1),
        )

        annotationArgVisitor.visitEnumArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "enum" },
            EnumValue("MyEnum", "FOO"),
        )

        annotationArgVisitor.visitArrayArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "array" },
            ArrayValue(listOf(StringValue("foo"), StringValue("bar"))),
        )

        annotationArgVisitor.visitAnnotationArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "annotation" },
            withArg {
                it.kotlinMetadataAnnotation.className shouldBe "Foo"
            },
        )

        annotationArgVisitor.visitClassArgument(
            clazz,
            ofType<KotlinAnnotatable>(),
            annotation,
            withArg { it.name shouldBe "kClass" },
            ClassValue("kotlin/String"),
        )
    }
}
