package proguard.analysis

import io.kotest.assertions.throwables.shouldThrow
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
import proguard.classfile.visitor.NamedMethodVisitor
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.exception.ArrayStoreTypeException
import proguard.evaluation.exception.StackCategoryOneException
import proguard.evaluation.exception.StackGeneralizationException
import proguard.evaluation.exception.StackTypeException
import proguard.evaluation.exception.ValueTypeException
import proguard.evaluation.exception.VariableEmptySlotException
import proguard.evaluation.exception.VariableIndexOutOfBoundException
import proguard.evaluation.exception.VariableTypeException
import proguard.evaluation.util.jsonprinter.JsonPrinter
import proguard.evaluation.value.DetailedArrayValueFactory
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.TypedReferenceValueFactory

/**
 * These test check that various invalid code snippets correctly throw exceptions from the
 * `PartialEvaluator`.
 *
 * @see PartialEvaluator
 */
class PartialEvaluatorErrorsTest : FreeSpec({

    PartialEvaluator.ENABLE_NEW_EXCEPTIONS = true

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

            val tracker = JsonPrinter()
            val pe = PartialEvaluator.Builder.create().setStateTracker(tracker).build()
            shouldThrow<VariableEmptySlotException> {
                evaluateProgramClass(
                    programClass,
                    pe,
                    "test",
                    "()Ljava/lang/Object;",
                )
            }
        }

        "Changing stack size" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    val startLabel = it.createLabel()
                    val elseLabel = it.createLabel()
                    it.iconst(50)
                        .label(startLabel)
                        .dup()
                        .iconst_5()
                        // ifle checks first stack element against 0, thus popping only 1,
                        // The code however is constructed for if_icmple that pops 2 elements.
                        // Not popping 2 elements makes the stack size increase every loop iteration
                        .ifle(elseLabel)
                        .iconst_5()
                        .isub()
                        .goto_(startLabel)
                        .label(elseLabel)
                        .ireturn()
                }
                .programClass

            shouldThrow<StackGeneralizationException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator.Builder.create().build(),
                    "test",
                    "()I",
                )
            }
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

            shouldThrow<VariableTypeException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator.Builder.create().build(),
                    "test",
                    "()Ljava/lang/Object;",
                )
            }
        }

        "Variable types do not match, expect reference" {
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .iconst_0()
                        .areturn()
                }
                .programClass

            shouldThrow<StackTypeException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator.Builder.create().build(),
                    "test",
                    "()Ljava/lang/Object;",
                )
            }
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

            shouldThrow<StackTypeException> { evaluateProgramClass(programClass, PartialEvaluator(), "test", "()J") }
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

            shouldThrow<StackTypeException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator.Builder.create().build(),
                    "test",
                    "()I",
                )
            }
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

            shouldThrow<StackCategoryOneException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator.Builder.create().build(),
                    "test",
                    "()J",
                )
            }
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

            shouldThrow<StackTypeException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator.Builder.create().build(),
                    "test",
                    "()F",
                )
            }
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

            shouldThrow<VariableIndexOutOfBoundException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator(),
                    "test",
                    "()V",
                )
            }
        }

        "Load an int into an int array but mistakenly give object ref" {
            // Only throws an error when value factory is of type TypedReferenceValueFactory
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

            // Throws on sufficient valueFactory
            shouldThrow<ValueTypeException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator(TypedReferenceValueFactory()),
                    "test",
                    "()Ljava/lang/Object;",
                )
            }

            // Does not throw on basic value factory
            evaluateProgramClass(
                programClass,
                PartialEvaluator(),
                "test",
                "()Ljava/lang/Object;",
            )
        }

        "Load an int from an int array but mistakenly give object ref" {
            // Only throws an error when value factory is of type TypedReferenceValueFactory
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                    it
                        .aload_0()
                        .iconst_0()
                        .iaload()
                        .return_()
                }
                .programClass

            // Throws on sufficient valueFactory
            shouldThrow<ValueTypeException> {
                evaluateProgramClass(
                    programClass,
                    PartialEvaluator(TypedReferenceValueFactory()),
                    "test",
                    "()V",
                )
            }

            // Does not throw on basic value factory
            evaluateProgramClass(
                programClass,
                PartialEvaluator(),
                "test",
                "()V",
            )
        }

        "Load an int into an int array should work" {
            // Only throws an error when value factory is of type TypedReferenceValueFactory
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()Ljava/lang/Object;", 50) {
                    it
                        .iconst_1()
                        .newarray(Instruction.ARRAY_T_INT.toInt())
                        .dup()
                        .iconst_0()
                        .iconst_5()
                        .iastore()
                        .areturn()
                }
                .programClass

            evaluateProgramClass(
                programClass,
                PartialEvaluator(TypedReferenceValueFactory()),
                "test",
                "()Ljava/lang/Object;",
            )
        }

        "Load an int from an int array should work" {
            // Only throws an error when value factory is of type TypedReferenceValueFactory
            val programClass = buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()I", 50) {
                    it
                        .iconst_1()
                        .newarray(Instruction.ARRAY_T_INT.toInt())
                        .dup()
                        .iconst_0()
                        .iaload()
                        .ireturn()
                }
                .programClass

            evaluateProgramClass(
                programClass,
                PartialEvaluator(TypedReferenceValueFactory()),
                "test",
                "()I",
            )
        }

        "Store a reference in an a integer array" {
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
            PartialEvaluator.ENABLE_NEW_EXCEPTIONS = true

            shouldThrow<ArrayStoreTypeException> {
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
        }

        "Load a reference into reference array but mistakenly give object ref" {
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

            shouldThrow<ValueTypeException> {
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
        }
    }

    "should throw but works" - {
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

            evaluateProgramClass(
                programClass,
                PartialEvaluator.Builder.create().build(),
                "test",
                "()I",
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

            evaluateProgramClass(
                programClass,
                PartialEvaluator.Builder.create().build(),
                "test",
                "()V",
            )
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

            evaluateProgramClass(
                programClass,
                PartialEvaluator.Builder.create().build(),
                "test",
                "()V",
            )
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
            NamedMethodVisitor(
                methodName,
                methodDescriptor,
                AllAttributeVisitor(
                    AttributeNameFilter(Attribute.CODE, partialEvaluator),
                ),
            ),
        )
    }
