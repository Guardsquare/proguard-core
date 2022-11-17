/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.classfile.util

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

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
})
