package proguard.analysis

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.AccessConstants
import proguard.classfile.ClassConstants
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.VersionConstants
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.instruction.Instruction
import proguard.classfile.visitor.AllMethodVisitor
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.exception.VariableIndexOutOfBoundException
import proguard.evaluation.exception.VariableTypeException
import proguard.evaluation.formatter.MachinePrinter
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.ParticularValueFactory

/**
 * These test check that various invalid code snippets correctly throw exceptions from the
 * `PartialEvaluator`.
 *
 * @see PartialEvaluator
 */
class PartialEvaluatorErrorsTest : FreeSpec({

    /**
     * This is a list of code snippets on which the `PartialEvaluator` throws on error
     * but are in need of proper error messages with a concise and correct explanation of the error.
     */
    "Throws from partial evaluator" - {
        "Empty variable slot read" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .ldc("test")
                        .astore_0()
                        .aload_1()
                        .areturn()
                }
                .programClass

            val printer = MachinePrinter()
            val pe = PartialEvaluator.Builder.create().setExtraInstructionVisitor(
                printer,
            ).build()
            printer.setEvaluator(pe)
            evaluateProgramClass(
                programClass,
                pe,
                "test",
                "()Ljava/lang/Object;",
            )
        }

        "Entire PE lifecycle" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PRIVATE or AccessConstants.STATIC, "initializer", "()I", 50) {
                    it.iconst(50).ireturn()
                }
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    val startLabel = it.createLabel()
                    val elseLabel = it.createLabel()
                    val loadLabel = it.createLabel()
                    val endLabel = it.createLabel()
                    it
                        .invokestatic("PartialEvaluatorDummy", "initializer", "()I")
                        .label(startLabel)
                        .dup()
                        .iconst_5()
                        .ificmple(elseLabel)
                        .iconst_5()
                        .isub()
                        .goto_(startLabel)
                        .label(elseLabel)
                        .jsr(loadLabel)
                        .athrow()
                        .label(loadLabel)
                        .astore_1()
                        .aload_0()
                        .ret(1)
                        .label(endLabel)
                        .catchAll(startLabel, endLabel)
                        .iconst_5()
                        .ireturn()
                }
                .programClass

            val printer = MachinePrinter()
            val valueFactory = ParticularValueFactory()
            val pe = PartialEvaluator.Builder.create().setExtraInstructionVisitor(
                printer,
            ).setValueFactory(valueFactory).setInvocationUnit(ExecutingInvocationUnit(valueFactory)).setEvaluateAllCode(true).build()
            printer.setEvaluator(pe)
            evaluateProgramClass(
                programClass,
                pe,
                "test",
                "()I",
            )
        }

        "Variable types do not match" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .ldc("test")
                        .astore_0()
                        .iload_0() // Store an `a`, load an `i`, this cannot work.
                        .areturn()
                }
                .programClass

            shouldThrow<VariableTypeException> { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()Ljava/lang/Object;") }
        }

        "Stack types do not match instruction" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()J", 50) {
                    it
                        .iconst_3()
                        .iconst_3()
                        .iconst_1()
                        .iconst_1()
                        .lsub()
                        .lreturn()
                }
                .programClass

            shouldThrowAny { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()J") }
        }

        "Stack types do not match instruction - long interpreted as int" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    it
                        .lconst_1()
                        .isub()
                        .ireturn()
                }
                .programClass

            shouldThrowAny { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()I") }
        }

        "dup of long" {
            // Should not work (same for swap) since long is a category one value
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()J", 50) {
                    it
                        .lconst_1()
                        .dup()
                        .lreturn()
                }
                .programClass

            shouldThrowAny { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()J") }
        }

        "getfield but return the wrong type" {
            val programClass = buildClass()
                .addField(AccessConstants.PRIVATE, "INT_FIELD", "I")
                .addMethod(AccessConstants.PUBLIC, "test", "()F", 50) {
                    it
                        .aload_0()
                        .getfield("PartialEvaluatorDummy", "INT_FIELD", "I")
                        .freturn()
                }
                .programClass

            shouldThrowAny { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()F") }
        }

        "Variable index out of bound" {
            // There is no 50th variable. The amount of local variables has been limited to 2
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                    it
                        .ldc("test")
                        .astore(50)
                        .return_()
                }
                .programClass

            programClass.accept(
                NamedMethodVisitor(
                    "test",
                    "()V",
                    AllAttributeVisitor(
                        AttributeNameFilter(
                            Attribute.CODE,
                            object : AttributeVisitor {
                                override fun visitCodeAttribute(
                                    clazz: Clazz,
                                    method: Method,
                                    codeAttribute: CodeAttribute,
                                ) {
                                    codeAttribute.u2maxLocals = 2
                                }
                            },
                        ),
                    ),
                ),
            )

            shouldThrow<VariableIndexOutOfBoundException> { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()V") }
        }
    }

    /**
     * Some code snippets have been identified where we want the partial evaluator to throw an exception,
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
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .iconst_1()
                        .newarray(Instruction.ARRAY_T_INT.toInt())
                        .dup()
                        .iconst_0()
                        .aload_0()
                        .aastore()
                        .areturn()
                }
                .programClass

            evaluateProgramClass(
                programClass,
                PartialEvaluator(
                    ParticularValueFactory(
                        DetailedArrayValueFactory(),
                        ParticularReferenceValueFactory(),
                    ),
                ),
                "test",
                "()Ljava/lang/Object;",
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
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .iconst_1()
                        .newarray(Instruction.ARRAY_T_INT.toInt())
                        .aload_0()
                        .iconst_0()
                        .iconst_5()
                        .iastore()
                        .areturn()
                }
                .programClass

            evaluateProgramClass(
                programClass,
                PartialEvaluator(
                    ParticularValueFactory(
                        DetailedArrayValueFactory(),
                        ParticularReferenceValueFactory(),
                    ),
                ),
                "test",
                "()Ljava/lang/Object;",
            )
        }

        "Exiting a monitor when no monitor was active" {
            // This is a low priority issue
            // Right now nor the PGA nor the `PartialEvaluator` tracks the entering and existing of monitors
            // It could throw an error as we are trying to exit a monitor that was never created / entered.
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    it
                        .iconst_5()
                        .aload_0()
                        .monitorexit()
                        .ireturn()
                }
                .programClass

            evaluateProgramClass(programClass, PartialEvaluator(), "test", "()I")
        }

        "Index out of bound" {
            // The following should be able to throw an error when accessing an array with an index that is out of range
            //  A distinction needs to be made, what do you know about the index? Do you know about the type? Value? Range?
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .iconst_1()
                        .newarray(Instruction.ARRAY_T_INT.toInt())
                        .dup()
                        .iconst_5()
                        .iconst_0()
                        .iastore()
                        .areturn()
                }
                .programClass

            val valueFac = ParticularValueFactory(DetailedArrayValueFactory())
            evaluateProgramClass(
                programClass,
                PartialEvaluator(valueFac, BasicInvocationUnit(valueFac), false),
                "test",
                "()Ljava/lang/Object;",
            )
        }
    }

    "Prints a warning when requested by the user" - {
        "Illegal static" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                    it
                        .getstatic("java.lang.System", "out", "bingbong")
                        .ldc("Hello World!")
                        .invokevirtual("java.io.PrintStream", "println", "(Ljava/lang/String;)void")
                        .return_()
                }
                .programClass

            evaluateProgramClass(programClass, PartialEvaluator(), "test", "()V")
        }

        "getfield but the referenced field does not exist" {
            // `INT_NO_EXIST` is not an existing field but this is not an issue!
            // This is handled by the ClassReferenceInitializer (see commented lines).
            // It will print out a warning message about the non-existent link.
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                    it
                        .aload_0()
                        .getfield("PartialEvaluatorDummy", "INT_NO_EXIST", "I")
                        .istore_1()
                        .return_()
                }
                .programClass

            evaluateProgramClass(programClass, PartialEvaluator(), "test", "()V")
        }
    }
})

// Helper function to not have to put this code in each test
fun buildClass(): ClassBuilder {
    return ClassBuilder(
        VersionConstants.CLASS_VERSION_1_8,
        AccessConstants.PUBLIC,
        "PartialEvaluatorDummy",
        ClassConstants.NAME_JAVA_LANG_OBJECT,
    )
}

// A little helper to avoid having to write this for each test
val evaluateProgramClass =
    { programClass: ProgramClass, partialEvaluator: PartialEvaluator, methodName: String, methodDescriptor: String ->
        programClass.accept(
            AllMethodVisitor(
                AllAttributeVisitor(
                    AttributeNameFilter(Attribute.CODE, partialEvaluator),
                ),
            ),
        )
    }
