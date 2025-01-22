package proguard.classfile.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.ClassConstants.METHOD_TYPE_INIT
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.and
import proguard.testutils.match

class ArrayInitializationReplacerTest : BehaviorSpec({
    Given("A class with unreachable code") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "A.jbc",
                """
            version 1.8;
            public class A extends java.lang.Object {
                public static void quickie()
                {
                    return
                    // Unreachable code starts from here.
                    bipush 10
                    newarray int
                }
                public static void longie()
                {
                    iconst_1
                    pop
                    iconst_1
                    pop
                    bipush 10
                    newarray int // The visitor only visits methods with a newarray instruction.
                    pop
                    return
                }
            }
                """.trimIndent(),
            ),
        )
        When("An ArrayInitializationReplacer is applied to the unreachable code") {
            val arrayInitializationReplacer = ArrayInitializationReplacer()
            programClassPool.classesAccept(arrayInitializationReplacer)
            Then("The class should be unchanged") {
                val clazz = programClassPool.getClass("A")
                val method by lazy { clazz.findMethod("quickie", METHOD_TYPE_INIT) }
                with(clazz and method) {
                    match {
                        return_()
                        bipush(10)
                        newarray(10)
                    } shouldBe true
                }
            }

            // The ArrayInitializationReplacer contains a PartialEvaluator. When the same
            // PartialEvaluator is used across classes, its stackBefore array is reused. At first,
            // it's full of nulls. As it visits more classes, the stackBefore is populated with stack
            // frame objects, which are emptied when visiting a new class. So when checking an offset
            // the PE hasn't visited, it'll be either null or an empty stack frame object. This test
            // covers both cases by having a long method after the unreachable method, and running
            // through the class twice.
            And("The same ArrayInitializationReplacer is re-applied, having visited longer code") {
                programClassPool.classesAccept(arrayInitializationReplacer)
                Then("The class should be unchanged") {
                    val clazz = programClassPool.getClass("A")
                    val method by lazy { clazz.findMethod("quickie", METHOD_TYPE_INIT) }
                    with(clazz and method) {
                        match {
                            return_()
                            bipush(10)
                            newarray(10)
                        } shouldBe true
                    }
                }
            }
        }
    }
})
