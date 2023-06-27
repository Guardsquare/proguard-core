package proguard.testutils

import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.InstructionVisitor

class MethodInstructionCollector : AttributeVisitor, InstructionVisitor {

    private var _instructionList = mutableListOf<Instruction>()

    fun getInstructionList(): List<Instruction> {
        return _instructionList
    }

    fun reset() {
        _instructionList.clear()
    }

    override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {}

    override fun visitCodeAttribute(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?) {
        codeAttribute?.let {
            if (it.u4codeLength > 0) {
                it.instructionsAccept(clazz, method, this)
            }
        }
    }

    override fun visitAnyInstruction(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?, offset: Int, instruction: Instruction?) {
        instruction?.let { _instructionList.add(it) }
    }

    companion object {
        fun getMethodInstructions(clazz: Clazz?, method: Method?): List<Instruction> {
            val programClass = clazz as ProgramClass
            val programMethod = method as ProgramMethod
            val collector = MethodInstructionCollector()
            programMethod.attributesAccept(programClass, collector)
            return collector.getInstructionList()
        }
    }
}
