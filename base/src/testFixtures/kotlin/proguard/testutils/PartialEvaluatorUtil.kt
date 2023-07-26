package proguard.testutils

import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.LocalVariableTableAttribute
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.evaluation.PartialEvaluator

class PartialEvaluatorUtil {

    companion object {
        fun evaluate(
            className: String,
            methodName: String,
            methodDescriptor: String,
            classPool: ClassPool,
            partialEvaluator: PartialEvaluator
        ): Pair<ArrayList<Pair<Int, Instruction>>, HashMap<String, Int>> {
            val clazz = classPool.getClass(className) as ProgramClass
            val method = clazz.findMethod(methodName, methodDescriptor) as ProgramMethod

            val codeAttribute =
                method.attributes.slice(0 until method.u2attributesCount).find { it.getAttributeName(clazz) == Attribute.CODE } as CodeAttribute
            val localVarTableAttribute =
                codeAttribute.attributes.slice(0 until codeAttribute.u2attributesCount).find { it.getAttributeName(clazz) == Attribute.LOCAL_VARIABLE_TABLE } as LocalVariableTableAttribute?

            val instructions = ArrayList<Pair<Int, Instruction>>()
            codeAttribute.instructionsAccept(
                clazz, method,
                object : InstructionVisitor {
                    override fun visitAnyInstruction(
                        clazz: Clazz?,
                        method: Method?,
                        codeAttribute: CodeAttribute?,
                        offset: Int,
                        instruction: Instruction?
                    ) {
                        instruction?.let { instructions.add(Pair(offset, it)) }
                    }
                }
            )

            val variableTable = HashMap<String, Int>()
            localVarTableAttribute?.localVariablesAccept(
                clazz,
                method,
                codeAttribute
            ) { _, _, _, localVar ->
                variableTable[localVar.getName(clazz)] = localVar.u2index
            }

            partialEvaluator.visitCodeAttribute(clazz, method, codeAttribute)

            return Pair(instructions, variableTable)
        }
    }
}
