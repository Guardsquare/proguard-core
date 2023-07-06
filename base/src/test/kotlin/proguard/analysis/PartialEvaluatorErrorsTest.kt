package proguard.analysis

import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.ClassPool
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.util.ClassReferenceInitializer
import proguard.classfile.util.WarningPrinter
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.ValueFactory
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import java.io.PrintStream
import java.io.PrintWriter


class PartialEvaluatorErrorsTest: FreeSpec({
    "Throw a correct and descriptive error message for the following code snippets" - {

        val fastBuild = { impl: String ->
            ClassPoolBuilder.fromSource(
                AssemblerSource("EmptySlot.jbc",
                """                    
                    public class EmptySlot extends java.lang.Object {
                        public final int intField = 42;
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

        "anewarray" {
            val (programClassPool, _) = fastBuild("""
                    lconst_1
                    anewarray long
                    areturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "monitor" {
            // TODO: should this work?
            val (programClassPool, _) = fastBuild("""
                    iconst_5
                    aload_0
                    monitorexit
                    ireturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "swap" {
            val (programClassPool, _) = fastBuild("""
                    iconst_5
                    swap
                    ireturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "out of bound" {
            // TODO: we could defo detect that this will not work!
            val (programClassPool, _) = fastBuild("""
                iconst_1
                anewarray int
                dup
                iconst_5
                iconst_5
                iastore 
                areturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator(DetailedArrayValueFactory()))
        }

        "illegal reference" {
            val (programClassPool, _) = fastBuild("""
                    iconst_5
                    swap
                    ireturn
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "Illegal static" {
            // TODO: this should fail? bingbong does not exist - no clue who should detect
            val (programClassPool, _) = fastBuild("""
                    getstatic java.lang.System#bingbong out
                    ldc "Hello World!"
                    invokevirtual java.io.PrintStream#void println(java.lang.String)
                    return
                """.trimIndent())
            val valueFactory: ValueFactory = ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory())
            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory)
            val partialEvaluator = PartialEvaluator(
                valueFactory,
                invocationUnit,
                false
            )
            fastEval(programClassPool, partialEvaluator)
        }

        "Illegal bytecode" {
            val (programClassPool, _) = fastBuild("""
                    getstatic System#PrintStream out
                    apples
                    ldc "Hello World!"
                    invokevirtual PrintStream#void println(String)
                    return
                """.trimIndent())
            fastEval(programClassPool, PartialEvaluator())
        }

        "getfield but field has wrong type" {
            val (programClassPool) = fastBuild("""
                    aload_0
                    getfield EmptySlot#float INT_FIELD
                    ireturn
                """.trimIndent())

            fastEval(programClassPool, PartialEvaluator())
        }

        "getfield but field no exist" {
            // Not an issue! you do not care - handled by ClassReferenceInitializer (see commented lines)
            val (programClassPool, libaryClassPool) = fastBuild("""
                    aload_0
                    getfield EmptySlot#int INT_NO_EXIST
                    ireturn
                """.trimIndent())

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

        "Duplicate top value of an empty stack" {
            val (programClassPool, _) = fastBuild("""
                dup
            """.trimIndent())

            fastEval(programClassPool, PartialEvaluator())
        }

        "goto far" {
            val (programClassPool, _) = fastBuild("""
                goto jafar
            """.trimIndent())
        }

    }
})
