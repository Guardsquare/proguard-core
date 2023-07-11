package proguard.analysis

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.AccessConstants

/**
 * These test check that various invalid code snippets correctly throw exceptions from the StackSizeComputer
 */
class StackSizeComputerErrorsTest : FreeSpec({
    "Stack size becomes negative" {
        // The stack size will be negative in this snippet, because we used the wrong type operation
        shouldThrowAny {
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
        shouldThrowAny {
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
        shouldThrowAny {
            buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) { code ->
                    code
                        .dup()
                        .return_()
                }
                .programClass
        }
    }
})
