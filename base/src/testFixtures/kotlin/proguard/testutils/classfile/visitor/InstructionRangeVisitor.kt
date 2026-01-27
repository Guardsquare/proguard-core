package proguard.testutils.classfile.visitor

import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.InstructionVisitor

/**
 * Utility class to visit a range of instructions
 * @param startOffset the start offset of the range (inclusive)
 * @param endOffset the end offset of the range (inclusive)
 */
class InstructionRangeVisitor(private val startOffset: Int, private val endOffset: Int, private val delegate: InstructionVisitor) : InstructionVisitor {

    override fun visitAnyInstruction(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?, offset: Int, instruction: Instruction?) {
        if (offset in startOffset..endOffset) {
            instruction?.accept(clazz, method, codeAttribute, offset, delegate)
        }
    }
}
