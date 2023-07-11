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
})
