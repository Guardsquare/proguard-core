package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.PartialEvaluator
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder

class PartialEvaluatorErrorsTest: FreeSpec({

    "Throw a correct and descriptive error message for the following code snippets" - {
        "Empty variable slot read" {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                AssemblerSource(
                    "EmptySlot.jbc",
                    """
                    public class EmptySlot extends java.lang.Object {
                        public static void test()
                        {
                               ldc "test"
                               astore_0
                               aload_1
                               areturn

                        }
                    }
                """.trimIndent()
                )
            )

            val partialEvaluator = PartialEvaluator()

            programClassPool.classesAccept(
                "EmptySlot", NamedMethodVisitor(
                    "test", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(Attribute.CODE, partialEvaluator)
                    )
                )
            )
        }
    }
})
