package proguard.classfile.editor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT
import proguard.classfile.editor.ClassReferenceFixer.shortKotlinNestedClassName
import proguard.classfile.kotlin.KotlinMetadataAnnotation
import proguard.classfile.kotlin.KotlinTypeMetadata
import proguard.classfile.kotlin.visitor.AllKotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.KotlinAnnotationVisitor
import proguard.classfile.kotlin.visitor.ReferencedKotlinMetadataVisitor
import proguard.classfile.util.ClassRenamer
import proguard.classfile.visitor.AllMethodVisitor
import proguard.classfile.visitor.ClassNameFilter
import proguard.classfile.visitor.MultiClassVisitor
import testutils.ClassPoolBuilder
import testutils.KotlinSource

class ClassReferenceFixerTest : FreeSpec({
    "Kotlin nested class short names should be generated correctly" - {
        "with a valid Java name" {
            val referencedClass = ClassBuilder(55, PUBLIC, "OuterClass\$innerClass", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "innerClass", referencedClass) shouldBe "innerClass"
        }

        // dollar symbols are valid in Kotlin when surrounded by backticks `$innerClass`
        "with 1 dollar symbol" {
            val referencedClass = ClassBuilder(55, PUBLIC, "OuterClass\$\$innerClass", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "\$innerClass", referencedClass) shouldBe "\$innerClass"
        }

        "with multiple dollar symbols" {
            val referencedClass = ClassBuilder(55, PUBLIC, "OuterClass\$\$\$inner\$Class", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "\$\$inner\$Class", referencedClass) shouldBe "\$\$inner\$Class"
        }

        "when they have a new name" {
            val referencedClass = ClassBuilder(55, PUBLIC, "newOuterClass\$newInnerClass", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "innerClass", referencedClass) shouldBe "newInnerClass"
        }

        "when they have a new name with a package" {
            val referencedClass = ClassBuilder(55, PUBLIC, "mypackage/newOuterClass\$newInnerClass", NAME_JAVA_LANG_OBJECT).programClass
            shortKotlinNestedClassName("OuterClass", "innerClass", referencedClass) shouldBe "newInnerClass"
        }
    }

    "Kotlin annotations should be fixed correctly" - {
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

        with(programClassPool) {
            classesAccept(
                MultiClassVisitor(
                    ClassRenamer {
                        when (it.name) {
                            "MyTypeAnnotation" -> "MyRenamedTypeAnnotation"
                            "MyEnum" -> "MyRenamedEnum"
                            "Foo" -> "RenamedFoo"
                            else -> it.name
                        }
                    },
                    // Rename all the methods in the annotation class
                    ClassNameFilter(
                        "MyRenamedTypeAnnotation",
                        AllMethodVisitor(
                            ClassRenamer(
                                { it.name },
                                { clazz, member -> "renamed${member.getName(clazz).capitalize()}" }
                            )
                        )
                    )
                )
            )

            // The ClassReferenceFixer should rename everything correctly
            classesAccept(ClassReferenceFixer(false))
        }

        val annotationVisitor = spyk<KotlinAnnotationVisitor>()
        val fileFacadeClass = programClassPool.getClass("TestKt")

        programClassPool.classesAccept(ReferencedKotlinMetadataVisitor(AllKotlinAnnotationVisitor(annotationVisitor)))
        val annotation = slot<KotlinMetadataAnnotation>()

        "there should be 1 annotation visited" {
            verify(exactly = 1) { annotationVisitor.visitTypeAnnotation(fileFacadeClass, ofType(KotlinTypeMetadata::class), capture(annotation)) }
        }

        "the annotation class name should be correctly renamed" {
            annotation.captured.kmAnnotation.className shouldBe "MyRenamedTypeAnnotation"
            annotation.captured.referencedAnnotationClass shouldBe programClassPool.getClass("MyTypeAnnotation")
        }

        "the annotation field references should be correctly set" {
            with(programClassPool.getClass("MyTypeAnnotation")) {
                annotation.captured.referencedArgumentMethods shouldBe mapOf(
                    "renamedString" to findMethod("renamedString", "()Ljava/lang/String;"),
                    "renamedByte" to findMethod("renamedByte", "()B"),
                    "renamedChar" to findMethod("renamedChar", "()C"),
                    "renamedShort" to findMethod("renamedShort", "()S"),
                    "renamedInt" to findMethod("renamedInt", "()I"),
                    "renamedLong" to findMethod("renamedLong", "()J"),
                    "renamedFloat" to findMethod("renamedFloat", "()F"),
                    "renamedDouble" to findMethod("renamedDouble", "()D"),
                    "renamedBoolean" to findMethod("renamedBoolean", "()Z"),
                    "renamedUByte" to findMethod("renamedUByte", "()B"),
                    "renamedUShort" to findMethod("renamedUShort", "()S"),
                    "renamedUInt" to findMethod("renamedUInt", "()I"),
                    "renamedULong" to findMethod("renamedULong", "()J"),
                    "renamedEnum" to findMethod("renamedEnum", "()LMyRenamedEnum;"),
                    "renamedArray" to findMethod("renamedArray", "()[Ljava/lang/String;"),
                    "renamedAnnotation" to findMethod("renamedAnnotation", "()LRenamedFoo;"),
                    "renamedKClass" to findMethod("renamedKClass", "()Ljava/lang/Class;")
                )
            }
        }
    }
})
