package proguard.analysis

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import proguard.classfile.AccessConstants

/**
 * These test check that various invalid code snippets correctly throw exceptions from the CompactCodeAttributeComposer
 */
class CompactCodeAttributeComposerErrorsTest : FreeSpec({
    "`goto` unknown label" {
        shouldThrowAny {
            buildClass()
                .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                    it
                        .goto_(it.createLabel())
                        .return_()
                }
        }
    }

    // this does not throw an error for the moment
    "bipush with invalid operand label" {
        // `bipush` expects a byte value but 300 exceedes the maximum byte value (>255)
        buildClass()
            .addMethod(AccessConstants.PUBLIC, "test", "()V", 50) {
                it
                    .bipush(300)
                    .return_()
            }
    }
})
