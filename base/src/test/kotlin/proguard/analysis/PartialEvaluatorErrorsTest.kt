package proguard.analysis

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.*
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder


/**
 * The purpose of these tests is to find ProGuard Assembly snippets that will result in errors thrown by the
 * `PartialEvaluator`.
 *
 * Some other issues have been discovered not directly related to the PartialEvaluator. Mainly issues when the code is
 * first read.
 *
 * The logger should be able to figure out what the context is and provide context to the user that is debugging
 * @see PartialEvaluator
 */
class PartialEvaluatorErrorsTest : FreeSpec({
    val fastBuild = { impl: String ->
        ClassPoolBuilder.fromSource(
            AssemblerSource(
                "PartialEvaluatorDummy.jbc",
                """
                public class PartialEvaluatorDummy extends java.lang.Object {
                    public java.lang.Object test()
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
            "PartialEvaluatorDummy", NamedMethodVisitor(
                "test", "()Ljava/lang/Object;",
                AllAttributeVisitor(
                    AttributeNameFilter(Attribute.CODE, partialEvaluator)
                )
            )
        )
    }


    /**
     * This is a list of code snippets on which the `PartialEvaluator` throws on error
     * but are in need of proper error messages with a concise and correct explanation of the error.
     */
    "Throws from partial evaluator but should be formatted" - {
        "Empty variable slot read" {
            val (programClassPool, _) = fastBuild(
                """
                ldc "test"
                astore_0
                aload_1
                areturn
                """.trimIndent()
            )

            shouldThrowAny { fastEval(programClassPool, PartialEvaluator()) }
        }

        "Variable types do not match" {
            // Store an `a`, load an `i`, this cannot work.
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

        "Variable types do not match instruction" {
            val (programClassPool, _) = fastBuild(
                """
                iconst_3
                iconst_3
                iconst_1
                iconst_1
                lsub
                aload_0
                areturn
                """.trimIndent()
            )

            shouldThrowAny { fastEval(programClassPool, PartialEvaluator()) }
        }

        "bipush with invalid operand label" {
            // `bipush` excpects a byte value but 300 exceedes the maximum byte value (>255)
            shouldThrowAny {
                fastBuild(
                    """
                    bipush 300
                    aload_0
                    areturn
                    """.trimIndent()
                )
            }
        }

        "Variable types do not match instruction - long interpreted as int" {
            val (programClassPool, _) = fastBuild(
                """
                lconst_1
                isub
                aload_0
                areturn
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
                areturn
                """.trimIndent()
            )

            shouldThrow<IllegalArgumentException> { fastEval(programClassPool, PartialEvaluator()) }
        }

        "Variable types do not match - assembler would throw because of negative stack size" {
            shouldThrowAny {
                ClassBuilder(
                    VersionConstants.CLASS_VERSION_1_8,
                    AccessConstants.PUBLIC,
                    "PartialEvaluatorDummy",
                    ClassConstants.NAME_JAVA_LANG_OBJECT
                )
                    .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) { code ->
                        code
                            .iconst_3()
                            .iconst_1()
                            .lsub()
                            .return_()
                    }
                    .programClass
            }
        }

        "Swap operation on a too small stack" {
            shouldThrowAny {
                ClassBuilder(
                    VersionConstants.CLASS_VERSION_1_8,
                    AccessConstants.PUBLIC,
                    "PartialEvaluatorDummy",
                    ClassConstants.NAME_JAVA_LANG_OBJECT
                )
                    .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) { code ->
                        code
                            .iconst_5()
                            .swap()
                            .return_()
                    }
                    .programClass
            }
        }

        "Dup operation on an empty stack" {
            shouldThrowAny {
                ClassBuilder(
                    VersionConstants.CLASS_VERSION_1_8,
                    AccessConstants.PUBLIC,
                    "PartialEvaluatorDummy",
                    ClassConstants.NAME_JAVA_LANG_OBJECT
                )
                    .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) { code ->
                        code
                            .dup()
                            .return_()
                    }
                    .programClass
            }
        }

        "`goto` unknown label" {
            shouldThrowAny {
                ClassBuilder(
                    VersionConstants.CLASS_VERSION_1_8,
                    AccessConstants.PUBLIC,
                    "PartialEvaluatorDummy",
                    ClassConstants.NAME_JAVA_LANG_OBJECT
                )
                    .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                        it
                            .goto_(it.createLabel())
                            .return_()
                    }
            }
        }

        "Variable index out of bound" {
            // There is no 50th variable. The amount of local variables has been limited to 2
            val (programClassPool, _) = fastBuild(
                """
                ldc "test"
                astore 50
                aload_0
                areturn
                """.trimIndent()
            )

            programClassPool.classesAccept(
                "PartialEvaluatorDummy", NamedMethodVisitor(
                    "test", "()Ljava/lang/Object;",
                    AllAttributeVisitor(
                        AttributeNameFilter(Attribute.CODE, object : AttributeVisitor {
                            override fun visitCodeAttribute(
                                clazz: Clazz,
                                method: Method,
                                codeAttribute: CodeAttribute
                            ) {
                                codeAttribute.u2maxLocals = 2
                            }
                        })
                    )
                )
            )
            shouldThrowAny { fastEval(programClassPool, PartialEvaluator()) }
        }
    }

    /**
     * Some other cases have been identified where the ProGuard Assembler throws an exception
     * but the exceptions could also do with some formatting and are not always helpful or entirely correct.
     */
    "Throws from assembler but should be a formatted message" - {
        "Variable types do not match instruction but negative stack size exception is thrown first" {
            // The same test has been built using the `CodeBuilder` in the previous section
            // The assembler uses the same methods and throws the same error, but it is wrapped in a `IOException`
            shouldThrowAny {
                fastBuild(
                    """
                    iconst_3
                    iconst_1
                    lsub
                    aload_0
                    areturn
                    """.trimIndent()
                )
            }
        }

        "Swap needs two elements on the stack" {
            // The same test has been built using the `CodeBuilder` in the previous section
            // The assembler uses the same methods and throws the same error, but it is wrapped in a `IOException`
            shouldThrowAny {
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
            // The same test has been built using the `CodeBuilder` in the previous section
            // The assembler uses the same methods and throws the same error, but it is wrapped in a `IOException`
            shouldThrowAny {
                fastBuild(
                    """
                    dup
                    """.trimIndent()
                )
            }
        }

        "Illegal bytecode instruction" {
            // `apples` is not a valid bytecode instructions and this should be clearly indicated by the PGA
            // This is an issue for the assembler and not for the partial evaluator
            // See: https://github.com/Guardsquare/proguard-assembler/issues/8
            shouldThrowAny {
                fastBuild(
                    """
                    apples
                    aload_0
                    areturn
                    """.trimIndent()
                )
            }
        }

        "`goto` to an invalid position" {
            // Jumping to an invalid label is caught by the PGA
            // The `PartialEvaluator` should do the same, see this test built with the ClassBuilder
            // in "Throws from partial evaluator but should be formatted"
            shouldThrowAny {
                fastBuild(
                    """
                    goto jafar
                    """.trimIndent()
                )
            }
        }
    }

    /**
     * Some code snippets have been identified where we want the partial evaluator / assembler to throw an exception,
     * but they don't
     */
    "Should throw but works" - {
        "Store a reference in an a integer array" {
            // From the PartialEvaluator debug output we can see that it is possible to store a reference into an
            // integer array. This should not pass!
            // [5] aload_0 v0
            // Vars:  [P0:LPartialEvaluatorDummy;!#0]
            // Stack: [3:1:[I?=![1]#0{0}][3:1:[I?=![1]#0{0}][4:0][5:LPartialEvaluatorDummy;!#0]
            // [6] aastore
            //         Vars:  [P0:LPartialEvaluatorDummy;!#0]
            // Stack: [3:1:[I?=![1]#0{LPartialEvaluatorDummy;!#0}]
            val (programClassPool, _) = fastBuild(
                """
                iconst_1
                newarray int
                dup
                iconst_0
                aload_0
                aastore
                areturn
                """.trimIndent()
            )
            fastEval(
                programClassPool,
                PartialEvaluator(ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory()))
            )
        }

        "Load a reference into reference array but mistakenly give object ref" {
            // It is possible to perform the iastore instruction when the `arrayref` isn't actually an array reference.
            // In this example the reference is a reference to the class itself and this is no issue according
            // to the PartialEvaluator:
            // [5] iconst_5
            //  Vars:  [P0:LPartialEvaluatorDummy;!#0]
            //  Stack: [1:[I?=![1]#0{0}][3:LPartialEvaluatorDummy;!#0][4:0][5:5]
            // [6] iastore
            //  Vars:  [P0:LPartialEvaluatorDummy;!#0]
            //  Stack: [1:[I?=![1]#0{0}]
            // [7] areturn
            //      is branching to :
            //  Vars:  [P0:LPartialEvaluatorDummy;!#0]
            //  Stack:
            val (programClassPool, _) = fastBuild(
                """
                iconst_1
                newarray int
                aload_0
                iconst_0
                iconst_5
                iastore
                areturn
                """.trimIndent()
            )
            fastEval(
                programClassPool,
                PartialEvaluator(ParticularValueFactory(DetailedArrayValueFactory(), ParticularReferenceValueFactory()))
            )
        }

        "Exiting a monitor when no monitor was active" {
            // This is a low priority issue
            // Right now nor the PGA nor the `PartialEvaluator` tracks the entering and existing of monitors
            // It could throw an error as we are trying to exit a monitor that was never created / entered.
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
            // The following should be able to thow an error when accessing an area with an index that is out of range
            //  A distintion needs to be made, what do you know about the index? Do you know about the type? Value? Range?
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

            val valueFac = ParticularValueFactory(DetailedArrayValueFactory())
            fastEval(programClassPool, PartialEvaluator(valueFac, BasicInvocationUnit(valueFac), false))
        }
    }

    "Prints a waning when requested by the user" - {
        "Illegal static" {
            // `bingbong` is not an existing static but this is not an issue!
            // This is handled by the ClassReferenceInitializer (see commented lines).
            // It will print out a warning message about the non-existent link.
            val (programClassPool, _) = fastBuild(
                """
                getstatic java.lang.System#bingbong out
                ldc "Hello World!"
                invokevirtual java.io.PrintStream#void println(java.lang.String)
                aload_0
                areturn
                """.trimIndent()
            )

//            val writer = PrintWriter(PrintStream(System.out))
//            val printer = WarningPrinter(writer)
//            programClassPool.classesAccept(
//                ClassReferenceInitializer(programClassPool, libaryClassPool, printer, printer, printer, printer)
//            )
//            printer.print("apple", "small tester", )
//            writer.flush()

            fastEval(programClassPool, PartialEvaluator())
        }

        "getfield but the referenced field field no exist" {
            // `INT_NO_EXIST` is not an existing field but this is not an issue!
            // This is handled by the ClassReferenceInitializer (see commented lines).
            // It will print out a warning message about the non-existent link.
            val (programClassPool, _) = fastBuild(
                """
                aload_0
                getfield EmptySlot#int INT_NO_EXIST
                istore_1
                aload_0
                areturn
                """.trimIndent()
            )

//            val writer = PrintWriter(PrintStream(System.out))
//            val printer = WarningPrinter(writer)
//            programClassPool.classesAccept(
//                ClassReferenceInitializer(programClassPool, libaryClassPool, printer, printer, printer, printer)
//            )
//            printer.print("apple", "small tester", )
//            writer.flush()

            fastEval(programClassPool, PartialEvaluator())
        }
    }
})
