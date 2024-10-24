package proguard.classfile.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.ClassConstants.METHOD_TYPE_INIT
import proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT
import proguard.classfile.VersionConstants.CLASS_VERSION_1_6
import proguard.classfile.editor.ClassBuilder
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.and
import proguard.testutils.match

class InstructionSequenceMatcherTest : BehaviorSpec({
    Given("A class that was compiled from sources") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "A.java",
                """
                public class A {
                    
                    public void b() {
                        byte[] b = new byte[]{ 97 };
                    }
                    
                }
                """.trimIndent(),
            ),
        )
        When("We run the ArrayInitializationReplacer") {
            programClassPool.classesAccept(ArrayInitializationReplacer())
            Then("The InstructionSequenceMatcher should match the PrimitiveArrayConstant") {
                val clazz = programClassPool.getClass("A")
                val method by lazy { clazz.findMethod("b", METHOD_TYPE_INIT) }

                with(clazz and method) {
                    match {
                        ldc("a".toByteArray())
                    } shouldBe true
                }
            }
        }
    }
    Given("A ProgramClass with a ldc(primitiveArray) instruction") {
        val clazz = ClassBuilder(CLASS_VERSION_1_6, PUBLIC, "A", NAME_JAVA_LANG_OBJECT).run {
            addMethod(PUBLIC, "b", METHOD_TYPE_INIT, 50) {
                it.ldc("The answer is 42".toByteArray())
                it.pop()
                it.return_()
            }
            programClass
        }
        Then("The InstructionSequenceMatcher should match the PrimitiveArrayConstant") {
            val method by lazy { clazz.findMethod("b", METHOD_TYPE_INIT) }
            with(clazz and method) {
                match {
                    ldc("The answer is 42".toByteArray())
                } shouldBe true
            }
        }
    }
})
