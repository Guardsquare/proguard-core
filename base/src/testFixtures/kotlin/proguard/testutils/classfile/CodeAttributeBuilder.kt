package proguard.testutils.classfile

import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.editor.CodeAttributeComposer
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.instruction.Instruction

class CodeAttributeBuilder {
    companion object {
        fun fromInstructionArray(clazz: ProgramClass, method: ProgramMethod, instructions: Array<Instruction>): CodeAttribute {
            val codeAttribute = CodeAttribute(0)

            val codeComposer = CodeAttributeComposer()

            codeComposer.beginCodeFragment(instructions.size)
            for (index in instructions.indices) {
                codeComposer.appendInstruction(index, instructions[index])
            }
            codeComposer.endCodeFragment()
            codeComposer.visitCodeAttribute(clazz, method, codeAttribute)
            val editor = ConstantPoolEditor(clazz)
            codeAttribute.u2attributeNameIndex = editor.addUtf8Constant("Code")

            return codeAttribute
        }
    }
}
