package proguard.analysis

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.ClassPool
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.InstructionEvaluationException
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import java.io.IOException


/**
 * The purpose of these tests is to find ProGuard Assembly snippets that will result in errors thrown by the
 * `PartialEvaluator`.
 *
 * The logger should be able to figure out what the context is and provide context to the user that is debugging
 * @see PartialEvaluator
 */
class PartialEvaluatorErrorsTest : FreeSpec({
    "Throw a correct and descriptive error message for the following cases" - {

        val fastBuild = { impl: String ->
            ClassPoolBuilder.fromSource(
                AssemblerSource(
                    "EmptySlot.jbc",
                    """
                    public class EmptySlot extends java.lang.Object {
                        public void test()
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

        "NullPointerExceptions" - {
            "Empty variable slot read" {
                val (programClassPool, _) = fastBuild(
                    """
                    ldc "test"
                    astore_0
                    aload_1
                    areturn
                """.trimIndent()
                )

                shouldThrow<InstructionEvaluationException> { fastEval(programClassPool, PartialEvaluator()) }
            }
        }

        "IllegalArgumentExceptions" - {
            "Variable types do not match" {
                val (programClassPool, _) = fastBuild(
                    """
                    ldc "test"
                    astore_0
                    iload_0
                    areturn
                """.trimIndent()
                )
                shouldThrow<IllegalArgumentException> { fastEval(programClassPool, PartialEvaluator()) }
            }

            "Variable types do not match instruction but negative stack size exception is thrown first" {
                // TODO: I feel like a better error can be given than negative stack size (see next test)
                //   Probably not fixable because stack size is checked by assembler
                shouldThrow<IOException> {
                    fastBuild(
                        """
                    iconst_3
                    iconst_1
                    lsub
                    lreturn
                """.trimIndent()
                    )
                }
            }

            "Variable types do not match instruction" {
                val (programClassPool, _) = fastBuild(
                    """
                    iconst_3
                    iconst_3
                    iconst_1
                    iconst_1
                    lsub
                    lreturn
                """.trimIndent()
                )
                shouldThrow<IllegalArgumentException> { fastEval(programClassPool, PartialEvaluator()) }
            }

            "Variable types do not match instruction - long interpreted as int" {
                val (programClassPool, _) = fastBuild(
                    """
                    lconst_1
                    isub
                    ireturn
                """.trimIndent()
                )
                shouldThrow<IllegalArgumentException> { fastEval(programClassPool, PartialEvaluator()) }
            }

            "anewarray" {
                val (programClassPool, _) = fastBuild(
                    """
                    lconst_1
                    anewarray long
                    areturn
                """.trimIndent()
                )
                shouldThrow<IllegalArgumentException> { fastEval(programClassPool, PartialEvaluator()) }
            }

            "getfield but field has wrong type" {
                val (programClassPool) = fastBuild(
                    """
                    aload_0
                    getfield EmptySlot#float INT_FIELD
                    ireturn
                """.trimIndent()
                )

                shouldThrow<IllegalArgumentException> { fastEval(programClassPool, PartialEvaluator()) }
            }
        }

        "Stack size too small" - {
            // The errors come from the assembler and not the partial evaluator
            "swap needs two elements on the stack" {
                shouldThrow<IOException> {
                    fastBuild(
                        """
                    iconst_5
                    swap
                    ireturn
                """.trimIndent()
                    )
                }
            }

            "Duplicate top value of an empty stack" {
                // The assembler already throws an error here
                shouldThrow<IOException> {
                    fastBuild(
                        """
                dup
            """.trimIndent()
                    )
                }
            }
        }


        "Exiting a monitor when no monitor was active" {
            // TODO: should this work?
            val (programClassPool, _) = fastBuild(
                """
                    iconst_5
                    aload_0
                    monitorexit
                    ireturn
                """.trimIndent()
            )
            fastEval(programClassPool, PartialEvaluator())
        }

        "index out of bound" {
            // TODO: we could defo detect that this will not work!
            val (programClassPool, _) = fastBuild(
                """
                iconst_1
                anewarray int
                dup
                iconst_5
                iconst_5
                iastore 
                areturn
                """.trimIndent()
            )
            fastEval(programClassPool, PartialEvaluator(DetailedArrayValueFactory()))
        }

        "Illegal static" {
            // TODO: this should fail? Printream does not exist - no clue who should detect
            val (programClassPool, _) = fastBuild(
                """
                    getstatic java.lang.System#Printream out
                    ldc "Hello World!"
                    invokevirtual java.io.PrintStream#void println(java.lang.String)
                    return
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "Illegal bytecode instruction" {
            // This is an issue for the assembler and not for the partial evaluator
            // See: https://github.com/Guardsquare/proguard-assembler/issues/8
            shouldThrow<IOException> {
                fastBuild(
                    """
                    getstatic System#PrintStream out
                    apples
                    ldc "Hello World!"
                    invokevirtual PrintStream#void println(String)
                    return
                """.trimIndent()
                )
            }
        }

        "getfield but the referenced field field no exist" {
            // Not an issue! This is handled by the ClassReferenceInitializer (see commented lines)
            // It will print out a warning message about the non-existent link
            val (programClassPool, libaryClassPool) = fastBuild(
                """
                    aload_0
                    getfield EmptySlot#int INT_NO_EXIST
                    ireturn
                """.trimIndent()
            )


            /*
            val writer = PrintWriter(PrintStream(System.out))
            val printer = WarningPrinter(writer)
            programClassPool.classesAccept(
                ClassReferenceInitializer(programClassPool, libaryClassPool, printer, printer, printer, printer)
            )
            printer.print("apple", "small tester", )
            writer.flush()
            */


            fastEval(programClassPool, PartialEvaluator())
        }

        "`goto` to an invalid position" {
            // The assembler already catches this
            shouldThrow<IOException> {
                fastBuild(
                    """
                goto jafar
            """.trimIndent()
                )
            }
        }

    }
})
