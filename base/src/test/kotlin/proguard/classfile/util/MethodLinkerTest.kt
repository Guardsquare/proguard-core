/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.classfile.util

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.constant.AnyMethodrefConstant
import proguard.classfile.constant.Constant
import proguard.classfile.constant.visitor.AllConstantVisitor
import proguard.classfile.constant.visitor.ConstantVisitor
import proguard.classfile.editor.CodeAttributeEditor
import proguard.classfile.editor.InstructionSequenceBuilder
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.KotlinSource

class MethodLinkerTest : FreeSpec({
    "Given a super class with a package-private final method that is also defined in a subclass" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Super.java",
                """
                    package a;
                    public class Super {
                        final void foo() { }
                    }
                """.trimIndent()
            ),
            JavaSource(
                "Foo.java",
                """
                    public class Foo extends a.Super {
                        public final void foo() { }
                    }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8")
        )

        "When the subclass is repackaged" - {
            programClassPool.getClass("Foo").accept(ClassRenamer { "a/Foo" })

            "Then the super-method is not linked to the sub-method" {
                programClassPool.classesAccept(MethodLinker())
                programClassPool.getClass("Foo").findMethod("foo", null).processingInfo shouldBe null
            }
        }
    }

    "Given Kotlin multi-file facade" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Foo.kt",
                """
                    @file:JvmName("FooBar")
                    @file:JvmMultifileClass

                    fun foo() { }
                """.trimIndent()
            ),
            KotlinSource(
                "Bar.kt",
                """
                    @file:JvmName("FooBar")
                    @file:JvmMultifileClass

                    fun bar() { }
                """.trimIndent()
            ),
        )

        "When the methods are linked" - {
            programClassPool.classesAccept(MethodLinker())

            val FooBar = programClassPool.getClass("FooBar")
            val foo = FooBar.findMethod("foo", null)
            val bar = FooBar.findMethod("bar", null)

            val FooBar__FooKt = programClassPool.getClass("FooBar__FooKt")
            val FooBar__BarKt = programClassPool.getClass("FooBar__BarKt")

            val fooDelegate = FooBar__FooKt.findMethod("foo", null)
            val barDelegate = FooBar__BarKt.findMethod("bar", null)

            "Then the delegates should be linked to the methods" {
                MethodLinker.lastMember(fooDelegate) shouldBe foo
                MethodLinker.lastMember(barDelegate) shouldBe bar
            }
        }
    }

    "Given Kotlin multi-file facade with uninitialized references" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Foo.kt",
                """
                    @file:JvmName("FooBar")
                    @file:JvmMultifileClass

                    fun foo() { }
                """.trimIndent()
            ),
            KotlinSource(
                "Bar.kt",
                """
                    @file:JvmName("FooBar")
                    @file:JvmMultifileClass

                    fun bar() { }
                """.trimIndent()
            ),
        )

        "When the methods are linked and the references are not initialized correctly" - {
            val FooBar = programClassPool.getClass("FooBar")

            FooBar.accept(
                AllConstantVisitor(object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {
                    }

                    override fun visitAnyMethodrefConstant(clazz: Clazz, anyMethodrefConstant: AnyMethodrefConstant) {
                        anyMethodrefConstant.referencedMethod = null
                    }
                })
            )

            "Then there should be no NullPointerException" {
                shouldNotThrow<NullPointerException> {
                    programClassPool.classesAccept(MethodLinker())
                }
            }
        }
    }

    "Given Kotlin multi-file facade with methods that contain injected code" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            KotlinSource(
                "Foo.kt",
                """
                    @file:JvmName("FooBar")
                    @file:JvmMultifileClass

                    fun foo() { }
                """.trimIndent()
            ),
            JavaSource(
                "Baz.java",
                """
                    public class Baz {
                        public void baz() { }
                    }
                """.trimIndent()
            )
        )

        "When the methods are linked" - {
            val FooBar = programClassPool.getClass("FooBar") as ProgramClass
            val Baz = programClassPool.getClass("Baz")
            val foo = FooBar.findMethod("foo", null)
            val baz = programClassPool.getClass("Baz").findMethod("baz", null)

            val codeAttributeEditor = CodeAttributeEditor(true, true)
            val isb = InstructionSequenceBuilder(FooBar, programClassPool, libraryClassPool)

            codeAttributeEditor.reset(100)
            codeAttributeEditor.insertBeforeOffset(
                0,
                isb.apply {
                    new_("Baz")
                    dup()
                    invokespecial("Bar", "<init>", "()V", Baz, Baz.findMethod("<init>", "()V"))
                    invokevirtual("Baz", "baz", "()V", Baz, baz)
                }.instructions()
            )

            foo.accept(FooBar, AllAttributeVisitor(codeAttributeEditor))

            programClassPool.classesAccept(MethodLinker())

            "Then the baz method should not be linked" {
                baz.processingInfo shouldBe null
            }
        }
    }
})
