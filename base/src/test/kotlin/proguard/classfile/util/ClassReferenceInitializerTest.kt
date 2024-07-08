/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.util

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.AccessConstants
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.JavaConstants
import proguard.classfile.Member
import proguard.classfile.Method
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMember
import proguard.classfile.ProgramMethod
import proguard.classfile.VersionConstants
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.EnclosingMethodAttribute
import proguard.classfile.attribute.SignatureAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.editor.AttributesEditor
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.editor.LibraryClassBuilder
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
import proguard.testutils.JavaSource
import proguard.testutils.KotlinSource
import java.util.function.Predicate
import proguard.classfile.kotlin.KotlinConstants.dummyClassPool as kotlinDummyClassPool

class ClassReferenceInitializerTest : BehaviorSpec({

    Given("Kotlin annotations should be initialized correctly") {
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
                """.trimIndent(),
            ),
            kotlincArguments = listOf("-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"),
        )

        // Note: the `ClassReferenceInitializer` is called by `ClassPoolBuilder.fromSource`
        // when creating a class pool, so no need to call it again in the test.

        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(ReferencedKotlinMetadataVisitor(AllKotlinAnnotationVisitor(annotationVisitor)))
        val annotation = slot<KotlinAnnotation>()

        Then("There should be 1 annotation visited") {
            verify(exactly = 1) {
                annotationVisitor.visitTypeAnnotation(fileFacadeClass, ofType(KotlinTypeMetadata::class), capture(annotation))
            }
        }

        Then("The annotation referenced class should be correct") {
            annotation.captured.className shouldBe "MyTypeAnnotation"
            annotation.captured.referencedAnnotationClass shouldBe programClassPool.getClass("MyTypeAnnotation")
        }

        Then("The annotation argument value references should be correctly set") {

            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            annotationArgVisitor,
                        ),
                    ),
                ),
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
                    ofType<Value>(),
                )
            }
        }

        Then("The class argument values should have their references correctly set") {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            KotlinAnnotationArgumentFilter(
                                Predicate<KotlinAnnotationArgument> { it.name == "kClass" },
                                annotationArgVisitor,
                            ),
                        ),
                    ),
                ),
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
                    },
                )
            }
        }

        Then("The enum argument values should have their references correctly set") {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            KotlinAnnotationArgumentFilter(
                                Predicate<KotlinAnnotationArgument> { it.name == "enum" },
                                annotationArgVisitor,
                            ),
                        ),
                    ),
                ),
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
                    },
                )
            }
        }

        Then("The annotation argument values should have their references correctly set") {
            val annotationArgVisitor = spyk<KotlinAnnotationArgumentVisitor>()

            programClassPool.classesAccept(
                ReferencedKotlinMetadataVisitor(
                    AllKotlinAnnotationVisitor(
                        AllKotlinAnnotationArgumentVisitor(
                            KotlinAnnotationArgumentFilter(
                                Predicate<KotlinAnnotationArgument> { it.name == "annotation" },
                                annotationArgVisitor,
                            ),
                        ),
                    ),
                ),
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
                                listOf(KotlinAnnotationArgument("string", StringValue("foo"))),
                            )
                    },
                )
            }
        }
    }

    Given("A Kotlin function with named types") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                // This snippet generates types annotated with `kotlin.ParameterName`:
                // the `ClassReferenceInitializer` should not crash when it cannot find
                // library classes.
                fun foo(p: (x: Int, y: Int) -> Unit) = p(1, 2)
                """.trimIndent(),
            ),
        )

        Then("An exception should not be thrown when library classes are missing") {
            shouldNotThrow<Exception> {
                programClassPool.classesAccept(ClassReferenceInitializer(programClassPool, ClassPool()))
            }
        }
    }

    Given("A companion object with a constant property") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Test.kt",
                """
                class BaseClass {
                    companion object {
                        val CONSTANT = "This does not need the base class"
                    }
                }
                """.trimIndent(),
            ),
            initialize = false,
        )

        Then("An exception should not be thrown if the base class is missing") {
            programClassPool.removeClass("BaseClass")
            programClassPool.classesAccept(
                KotlinMetadataInitializer { _, message ->
                    println(
                        message,
                    )
                },
            )
            shouldNotThrow<Exception> {
                programClassPool.classesAccept(
                    ClassReferenceInitializer(programClassPool, ClassPool()),
                )
            }
        }
    }

    Given("An annotation class containing annotations") {
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
                """.trimIndent(),
            ),
        )

        Then("The annotation synthetic method should be initialized correctly") {
            val visitor = spyk<KotlinPropertyVisitor>()
            programClassPool.classesAccept(
                "MyAnnotation",
                ReferencedKotlinMetadataVisitor(
                    AllPropertyVisitor(
                        KotlinPropertyFilter({ it.name == "foo" }, visitor),
                    ),
                ),
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
                        },
                    ),
                ),
            )

            syntheticAnnotationMethod shouldNotBe null

            verify(exactly = 1) {
                visitor.visitAnyProperty(
                    programClassPool.getClass("MyAnnotation"),
                    ofType<KotlinDeclarationContainerMetadata>(),
                    withArg {
                        it.syntheticMethodForAnnotations shouldBe MethodSignature(null, "foo\$annotations", "()V")
                        it.referencedSyntheticMethodForAnnotations shouldBe syntheticAnnotationMethod
                    },
                )
            }
        }
    }

    Given("A missing reference visitor") {
        val missingReferenceCrasher = object : InvalidReferenceVisitor {
            override fun visitMissingClass(referencingClazz: Clazz, reference: String) {
                if (!reference.startsWith(ClassUtil.internalClassName(JavaConstants.PACKAGE_JAVA_LANG))) {
                    throw RuntimeException("Missing reference: $reference")
                }
            }

            override fun visitProgramDependency(referencingClazz: Clazz, dependency: Clazz) {
                throw RuntimeException("Library class depending on program class: ${dependency.getName()}")
            }

            override fun visitAnyMissingMember(referencingClazz: Clazz, reference: Clazz, name: String, type: String) {
                throw RuntimeException("Missing member reference: ${reference.getName()}.$name")
            }

            override fun visitMissingEnclosingMethod(
                enclosingClazz: Clazz,
                reference: Clazz,
                name: String,
                type: String,
            ) {
                throw RuntimeException("Missing enclosing method reference: ${reference.getName()}.$name")
            }
        }

        When("There are no missing references") {
            Then("The missing reference visitor should not be called") {
                val (programClassPool, _) = ClassPoolBuilder.fromSource(
                    JavaSource(
                        "Main.java",
                        """
                    public class Main {
                        public static void main(String[] args ) {
                            Other other = new Other();
                            other.someField = 6;
                            other.someMethod();
                        }
                    }   
                        """.trimIndent(),
                    ),
                    JavaSource(
                        "Other.java",
                        """
                    public class Other {
                        public int someField;
                        
                        public void someMethod() {}
                    }
                        """.trimIndent(),
                    ),
                )

                val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true, missingReferenceCrasher)

                shouldNotThrowAny {
                    programClassPool.classesAccept(initializer)
                }
            }
        }

        When("A library class references a program class") {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "A.java",
                    """
                    public class A {                    
                    }   
                    """.trimIndent(),
                ),
            )

            val libraryClassPool = ClassPool()
            val builder = LibraryClassBuilder(AccessConstants.PUBLIC, "B", "A")
            builder.addField(AccessConstants.PUBLIC, "something", "LA;")
            libraryClassPool.addClass(builder.libraryClass)

            Then("visitProgramDependency should be invoked") {
                val referenceInitializer = ClassReferenceInitializer(programClassPool, libraryClassPool, true, missingReferenceCrasher)

                shouldThrowMessage("Library class depending on program class: A") {
                    // Test field ref.
                    libraryClassPool.classesAccept(referenceInitializer)
                }
            }
        }

        When("A class is missing") {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "Main.java",
                    """
                    public class Main {
                        public static void main(String[] args ) {
                            Other other = new Other();
                        }
                    }   
                    """.trimIndent(),
                ),
                JavaSource(
                    "Other.java",
                    """
                    public class Other {
                        
                    }
                    """.trimIndent(),
                ),
            )

            programClassPool.removeClass("Other")

            val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true, missingReferenceCrasher)

            Then("visitMissingClass should be invoked") {
                shouldThrowMessage("Missing reference: Other") { programClassPool.classesAccept(initializer) }
            }
        }

        When("A method is missing") {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "Main.java",
                    """
                    public class Main {
                        public static void main(String[] args ) {
                            Other other = new Other();
                            other.doSomething();
                        }
                    }   
                    """.trimIndent(),
                ),
                JavaSource(
                    "Other.java",
                    """
                    public class Other {
                        public void doSomething() {}
                    }
                    """.trimIndent(),
                ),
            )

            programClassPool.getClass("Other").methodAccept("doSomething", "()V", MemberRenamer { _, _ -> "doNothing" })

            val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true, missingReferenceCrasher)

            Then("visitAnyMissingMember should be invoked") {
                shouldThrowMessage("Missing member reference: Other.doSomething") { programClassPool.classesAccept(initializer) }
            }
        }

        When("A field is missing") {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "Main.java",
                    """
                    public class Main {
                        public static void main(String[] args ) {
                            Other other = new Other();
                            other.someField = 10;
                        }
                    }   
                    """.trimIndent(),
                ),
                JavaSource(
                    "Other.java",
                    """
                    public class Other {
                        public int someField;
                    }
                    """.trimIndent(),
                ),
            )

            programClassPool.getClass("Other").fieldAccept("someField", "I", MemberRenamer { _, _ -> "nothing" })

            val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true, missingReferenceCrasher)

            Then("visitAnyMissingMember should be invoked") {
                shouldThrowMessage("Missing member reference: Other.someField") { programClassPool.classesAccept(initializer) }
            }
        }

        When("An enclosing method is missing") {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "Main.java",
                    """
                    public class Main {
                    }   
                    """.trimIndent(),
                ),
            )

            val clazz = ClassBuilder(VersionConstants.CLASS_VERSION_17, AccessConstants.PUBLIC, "Test", "java/lang/Object").programClass
            val attributesEditor = AttributesEditor(clazz, false)
            val constantPoolEditor = ConstantPoolEditor(clazz)

            attributesEditor.addAttribute(
                EnclosingMethodAttribute(
                    constantPoolEditor.addUtf8Constant("EnclosingMethod"),
                    constantPoolEditor.addClassConstant("Main", null),
                    constantPoolEditor.addNameAndTypeConstant("iDontExist", "()V"),
                ),
            )

            programClassPool.addClass(clazz)

            val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true, missingReferenceCrasher)

            Then("visitMissingEnclosingMethod should be invoked") {
                shouldThrowMessage("Missing enclosing method reference: Main.iDontExist") {
                    programClassPool.classesAccept(initializer)
                }
            }
        }
    }

    Given("A class with a valid signature attribute") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    public class MyTest<T> {
                    }   
                """.trimIndent(),
            ),
            JavaSource(
                "Test.java",
                """
                    public class Test<T> extends MyTest<T> {}
                """.trimIndent(),
            ),
        )

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The referenced classes for the attribute should be set") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "Test",
                AllAttributeVisitor(object : AttributeVisitor {
                    override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                    override fun visitSignatureAttribute(clazz: Clazz?, signatureAttribute: SignatureAttribute?) {
                        if (signatureAttribute!!.referencedClasses[1].equals(programClassPool.getClass("MyTest"))) {
                            attributeFound = true
                        }
                    }
                }),
            )
            attributeFound shouldBe true
        }
    }

    Given("A class with an invalid signature attribute") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    public class MyTest<T> {
                    }   
                """.trimIndent(),
            ),
        )

        val clazz = ClassBuilder(VersionConstants.CLASS_VERSION_17, AccessConstants.PUBLIC, "Test", "MyTest").programClass
        val attributesEditor = AttributesEditor(clazz, false)
        val constantPoolEditor = ConstantPoolEditor(clazz)

        attributesEditor.addAttribute(
            SignatureAttribute(
                constantPoolEditor.addUtf8Constant("Signature"),
                constantPoolEditor.addUtf8Constant("<T:Ljava/lang/Object;>LMyTest<TT;>;<invalid>"),
            ),
        )

        programClassPool.addClass(clazz)

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The invalid attribute should be removed") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "Test",
                AllAttributeVisitor(object : AttributeVisitor {
                    override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                    override fun visitSignatureAttribute(clazz: Clazz?, signatureAttribute: SignatureAttribute?) {
                        attributeFound = true
                    }
                }),
            )
            attributeFound shouldBe false
        }
    }

    Given("A class with a valid signature attribute referencing an inner class") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    public class MyTest<T> {
                        public class InnerClass {}
                        public class Test extends InnerClass {}
                    }   
                """.trimIndent(),
            ),
        )

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The referenced classes for the attribute should be set") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "MyTest\$Test",
                AllAttributeVisitor(object : AttributeVisitor {
                    override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                    override fun visitSignatureAttribute(
                        clazz: Clazz?,
                        signatureAttribute: SignatureAttribute?,
                    ) {
                        if (programClassPool.getClass("MyTest")?.equals(signatureAttribute?.referencedClasses?.get(0)) == true &&
                            programClassPool.getClass("MyTest\$InnerClass")?.equals(signatureAttribute?.referencedClasses?.get(1)) == true
                        ) {
                            attributeFound = true
                        }
                    }
                }),
            )
            attributeFound shouldBe true
        }
    }

    Given("A class with a generic field") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    import java.util.List;
                    
                    public class MyTest {
                        public List<String> aList;
                    }   
                """.trimIndent(),
            ),
        )

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The signature attribute should not be removed") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "MyTest",
                AllMemberVisitor(
                    AllAttributeVisitor(object : AttributeVisitor {
                        override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                        override fun visitSignatureAttribute(
                            clazz: Clazz?,
                            member: Member?,
                            signatureAttribute: SignatureAttribute?,
                        ) {
                            if (signatureAttribute?.getSignature(clazz).equals("Ljava/util/List<Ljava/lang/String;>;")) {
                                attributeFound = true
                            }
                        }
                    }),
                ),
            )
            attributeFound shouldBe true
        }
    }

    Given("A class with a generic field with an invalid signature") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    import java.util.List;
                    
                    public class MyTest {
                        public List<String> aList;
                    }   
                """.trimIndent(),
            ),
        )

        val clazz = programClassPool.getClass("MyTest")
        val field = clazz.findField("aList", "Ljava/util/List;")

        val editor = AttributesEditor(clazz as ProgramClass?, field as ProgramMember?, true)
        editor.deleteAttribute("Signature")

        val constantPoolEditor = ConstantPoolEditor(clazz)

        editor.addAttribute(
            SignatureAttribute(
                constantPoolEditor.addUtf8Constant("Signature"),
                constantPoolEditor.addUtf8Constant("<T:Lja<invalid>"),
            ),
        )

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The signature attribute should be removed") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "MyTest",
                AllMemberVisitor(
                    AllAttributeVisitor(object : AttributeVisitor {
                        override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                        override fun visitSignatureAttribute(
                            clazz: Clazz?,
                            member: Member?,
                            signatureAttribute: SignatureAttribute?,
                        ) {
                            if (signatureAttribute?.getSignature(clazz).equals("Ljava/util/List<Ljava/lang/String;>;")) {
                                attributeFound = true
                            }
                        }
                    }),
                ),
            )
            attributeFound shouldBe false
        }
    }

    Given("A class with a generic method") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    import java.util.List;
                    
                    public class MyTest {
                        public List<String> getList() {
                            return null;
                        }
                    }   
                """.trimIndent(),
            ),
        )

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The signature attribute should not be removed") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "MyTest",
                AllMemberVisitor(
                    AllAttributeVisitor(object : AttributeVisitor {
                        override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                        override fun visitSignatureAttribute(
                            clazz: Clazz?,
                            member: Member?,
                            signatureAttribute: SignatureAttribute?,
                        ) {
                            if (signatureAttribute?.getSignature(clazz).equals("()Ljava/util/List<Ljava/lang/String;>;")) {
                                attributeFound = true
                            }
                        }
                    }),
                ),
            )
            attributeFound shouldBe true
        }
    }

    Given("A class with a generic method with an invalid signature") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "MyTest.java",
                """
                    import java.util.List;
                    
                    public class MyTest {
                        public List<String> getList() {
                            return null;
                        }
                    }   
                """.trimIndent(),
            ),
        )

        val clazz = programClassPool.getClass("MyTest")
        val method = clazz.findMethod("getList", "()Ljava/util/List;")

        val editor = AttributesEditor(clazz as ProgramClass?, method as ProgramMember?, true)
        editor.deleteAttribute("Signature")

        val constantPoolEditor = ConstantPoolEditor(clazz)

        editor.addAttribute(
            SignatureAttribute(
                constantPoolEditor.addUtf8Constant("Signature"),
                constantPoolEditor.addUtf8Constant("<T:Lja<invalid>"),
            ),
        )

        val initializer = ClassReferenceInitializer(programClassPool, ClassPool(), true)

        Then("The signature attribute should be removed") {
            programClassPool.classesAccept(initializer)
            var attributeFound = false
            programClassPool.classAccept(
                "MyTest",
                AllMemberVisitor(
                    AllAttributeVisitor(object : AttributeVisitor {
                        override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

                        override fun visitSignatureAttribute(
                            clazz: Clazz?,
                            member: Member?,
                            signatureAttribute: SignatureAttribute?,
                        ) {
                            if (signatureAttribute?.getSignature(clazz).equals("()Ljava/util/List<Ljava/lang/String;>;")) {
                                attributeFound = true
                            }
                        }
                    }),
                ),
            )
            attributeFound shouldBe false
        }
    }
})
