package proguard.classfile.editor

import io.kotest.core.spec.style.FunSpec
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.InstructionFactory

class CodeAttributeComposerTest : FunSpec({
    test("CodeAttributeComposer should allow code beyond its maxmimum fragment size") {
        val codeComposer = CodeAttributeComposer()
        codeComposer.reset()
        codeComposer.beginCodeFragment(0)
        codeComposer.appendInstruction(ClassEstimates.TYPICAL_CODE_LENGTH + 1, InstructionFactory.create(Instruction.OP_NOP))
        codeComposer.endCodeFragment()
    }
})
