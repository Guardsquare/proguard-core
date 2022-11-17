package proguard.classfile.editor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.FINAL
import proguard.classfile.AccessConstants.PROTECTED
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.util.ClassRenamer
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class AccessFixerTest : FreeSpec({
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

        "Then the super method access flags should not be modified incorrectly" {
            programClassPool.classesAccept(AccessFixer())
            programClassPool.getClass("Foo").findMethod("foo", null).accessFlags shouldBe (PUBLIC or FINAL)
            programClassPool.getClass("a/Super").findMethod("foo", null).accessFlags shouldBe FINAL
            programClassPool.getClass("a/Super").findMethod("foo", null).accessFlags.and(PUBLIC or PROTECTED) shouldBe 0
        }
    }

    "Given a super class with a referenced package-private final method that is also defined in a subclass" - {
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
                "Bar.java",
                """
                    package a;
                    public class Bar {
                       void bar() {
                           new Super().foo();
                       }
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

        "When the referencing class is repackaged" - {
            programClassPool.getClass("a/Bar").accept(ClassRenamer { "b/Bar" })

            "Then the super method access flags should not be modified incorrectly" {
                programClassPool.classesAccept(AccessFixer())
                programClassPool.getClass("Foo").findMethod("foo", null).accessFlags shouldBe (PUBLIC or FINAL)
                programClassPool.getClass("a/Super").findMethod("foo", null).accessFlags shouldBe FINAL
                programClassPool.getClass("a/Super").findMethod("foo", null).accessFlags.and(PUBLIC or PROTECTED) shouldBe 0
            }
        }
    }
})
