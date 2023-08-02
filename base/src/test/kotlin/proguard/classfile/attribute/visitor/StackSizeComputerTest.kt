package proguard.classfile.attribute.visitor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import proguard.analysis.buildClass
import proguard.classfile.AccessConstants
import proguard.classfile.exception.NegativeStackSizeException

class StackSizeComputerTest : FreeSpec({
    "Throws exceptions" - {
        StackSizeComputer.prettyInstructionBuffered = 7
        "Stack size becomes negative" {
            // The stack size will be negative in this snippet, because we used the wrong type operation
            shouldThrow<NegativeStackSizeException> {
                buildClass()
                    .addMethod(AccessConstants.PUBLIC, "test", "()J", 50) { code ->
                        code
                            .iconst_3()
                            .iconst_1()
                            .lsub()
                            .lreturn()
                    }
                    .programClass
            }
        }

        "Swap operation on a too small stack" {
            shouldThrow<NegativeStackSizeException> {
                buildClass()
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
            shouldThrow<NegativeStackSizeException> {
                buildClass()
                    .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) { code ->
                        code
                            .dup()
                            .return_()
                    }
                    .programClass
            }
        }
    }
})
