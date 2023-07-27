package proguard.testutils

import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.LocalVariableTableAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.attribute.visitor.AttributeNameFilter
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.attribute.visitor.MultiAttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.AllInstructionVisitor
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

            val instructions = ArrayList<Pair<Int, Instruction>>()
            val variableTable = HashMap<String, Int>()
            method.attributesAccept(
                clazz,
                AttributeNameFilter(
                    Attribute.CODE,
                    MultiAttributeVisitor(
                        partialEvaluator,
                        AllInstructionVisitor(object : InstructionVisitor {
                            override fun visitAnyInstruction(
                                clazz: Clazz,
                                method: Method,
                                codeAttribute: CodeAttribute,
                                offset: Int,
                                instruction: Instruction
                            ) {
                                instructions.add(Pair(offset, instruction))
                            }
                        }),
                        AllAttributeVisitor(object : AttributeVisitor {
                            override fun visitAnyAttribute(clazz: Clazz, attribute: Attribute) {}

                            override fun visitLocalVariableTableAttribute(
                                clazz: Clazz,
                                method: Method,
                                codeAttribute: CodeAttribute,
                                localVarTableAttribute: LocalVariableTableAttribute
                            ) {
                                localVarTableAttribute.localVariablesAccept(
                                    clazz,
                                    method,
                                    codeAttribute
                                ) { _, _, _, localVar ->
                                    variableTable[localVar.getName(clazz)] = localVar.u2index
                                }
                            }
                        })
                    )
                )
            )

            return Pair(instructions, variableTable)
        }
    }
}
