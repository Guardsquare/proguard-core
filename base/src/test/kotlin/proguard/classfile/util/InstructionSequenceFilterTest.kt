package proguard.classfile.util

import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.ClassConstants.NAME_JAVA_LANG_OBJECT
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.VersionConstants.CLASS_VERSION_1_6
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.editor.ClassBuilder
import proguard.classfile.editor.InstructionSequenceBuilder
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.testutils.and
import proguard.testutils.component1
import proguard.testutils.component2
import proguard.testutils.instructionsAccept

class InstructionSequenceFilterTest : BehaviorSpec({
    Given("A ProgramClass with some arithmetic") {
        val clazz = ClassBuilder(CLASS_VERSION_1_6, PUBLIC, "A", NAME_JAVA_LANG_OBJECT).run {
            addMethod(PUBLIC, "b", "()I", 50) {
                it.iconst_0()
                it.iconst_5()
                it.iadd()
                it.ireturn()
            }
            programClass
        }

        Then("The InstructionSequenceFilter should only delegate the matched instructions ") {
            val method by lazy { clazz.findMethod("b", "()I") }

            val (constants, instructions) = InstructionSequenceBuilder().iconst_5().iadd()

            val filterMatcher = InstructionSequenceMatcher(constants, instructions)
            val checkMatcher = InstructionSequenceMatcher(constants, instructions)

            var firstOffset = -1
            with(clazz and method) {
                instructionsAccept(
                    InstructionSequenceFilter(
                        filterMatcher,
                        object : InstructionVisitor {
                            override fun visitAnyInstruction(
                                clazz: Clazz,
                                method: Method,
                                codeAttribute: CodeAttribute,
                                offset: Int,
                                instruction: Instruction,
                            ) {
                                if (checkMatcher.isMatching) {
                                    fail("No more instructions should be visited after the first matching sequence")
                                }

                                instruction.accept(clazz, method, codeAttribute, offset, checkMatcher)

                                if (firstOffset < 0) {
                                    firstOffset = offset
                                }
                            }
                        },
                    ),
                )
            }

            filterMatcher.matchedInstructionOffset(0) shouldBe checkMatcher.matchedInstructionOffset(0)
            filterMatcher.instructionCount() shouldBe checkMatcher.instructionCount()
            firstOffset shouldBe filterMatcher.matchedInstructionOffset(0)
        }
    }
})
