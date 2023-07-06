package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.ClassPool
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.PartialEvaluator
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder

class PartialEvaluatorErrorsTest: FreeSpec({
    "Throw a correct and descriptive error message for the following code snippets" - {

        val fastBuild = { impl: String ->
            ClassPoolBuilder.fromSource(
                AssemblerSource("EmptySlot.jbc",
                """
                    public class EmptySlot extends java.lang.Object {
                        public static void test()
                        {
                               $impl
                        }
                    }
                """.trimIndent()
                )
            )
        }

        val fastEval = { pool: ClassPool, partialEvaluator: PartialEvaluator ->
            pool.classesAccept(
                "EmptySlot", NamedMethodVisitor(
                    "test", "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(Attribute.CODE, partialEvaluator)
                    )
                )
            )
        }

        "Empty variable slot read" {
            val (programClassPool, _) = fastBuild("""
                    ldc "test"
                    astore_0
                    aload_1
                    areturn
                """.trimIndent())

            fastEval(programClassPool, PartialEvaluator())
        }

        "Variable types do not match" {
            val (programClassPool, _) = fastBuild("""
                    ldc "test"
                    astore_0
                    iload_0
                    areturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "Variable types do not match instruction - not enough" {
            // TODO: I feel like a better error can be given than negative stack size (see next test)
            //   Probably not fixable because stack size is checked by assembler
            val (programClassPool, _) = fastBuild("""
                    iconst_3
                    iconst_1
                    lsub
                    lreturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "Variable types do not match instruction" {
            val (programClassPool, _) = fastBuild("""
                    iconst_3
                    iconst_3
                    iconst_1
                    iconst_1
                    lsub
                    lreturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "Variable types do not match instruction - long interpreted as int" {
            val (programClassPool, _) = fastBuild("""
                    lconst_1
                    isub
                    ireturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "Always taken" {
            // TODO: Maybe this is not an error but we could track this :)
        }

        "Duplicate top value of an empty stack" {
            val (programClassPool, _) = fastBuild("""
                dup
            """.trimIndent())

            fastEval(programClassPool, PartialEvaluator())
        }
    }
})
