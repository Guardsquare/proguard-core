package proguard.analysis

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.AccessConstants

/**
 * The purpose of these tests is to find test snippets that will result in errors thrown by the
 * @see proguard.classfile.attribute.visitor.StackSizeComputer
 *
 * The logger should be able to figure out what the context is and provide context to the user that is debugging.
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
