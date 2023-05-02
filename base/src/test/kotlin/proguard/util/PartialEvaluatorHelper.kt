package proguard.util

import proguard.classfile.ClassPool
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.constant.MethodrefConstant
import proguard.classfile.instruction.ConstantInstruction
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.InstructionFactory
import proguard.classfile.util.ClassUtil
import proguard.evaluation.ExecutingInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.ParticularReferenceValueFactory
import proguard.evaluation.value.ArrayReferenceValueFactory
import proguard.evaluation.value.ParticularReferenceValue
import proguard.evaluation.value.ParticularValueFactory
import proguard.evaluation.value.Value
import proguard.evaluation.value.ValueFactory

class PartialEvaluatorHelper {
    companion object {

        fun evaluateMethod(className: String, methodName: String, methodDescriptor: String, programClassPool: ClassPool): HashMap<Int, MethodWithStack> {
            val clazz = programClassPool.getClass(className) as ProgramClass

            val valueFactory: ValueFactory = ParticularValueFactory(ArrayReferenceValueFactory(), ParticularReferenceValueFactory())
            val invocationUnit = ExecutingInvocationUnit.Builder().build(valueFactory)
            val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, true)

            val codeAttribute = visitMethod(clazz, partialEvaluator, methodName, methodDescriptor)

            return collectInvocationsWithStack(clazz, partialEvaluator, codeAttribute)
        }

        fun visitMethod(clazz: ProgramClass, partialEvaluator: PartialEvaluator, methodName: String, methodDescriptor: String): CodeAttribute {
            val method = clazz.findMethod(methodName, methodDescriptor)
            if (method !is ProgramMethod) {
                throw AssertionError("Method $methodName.$methodDescriptor not found")
            }

            val codeAttribute = method.attributes.filter { x -> x.getAttributeName(clazz) == Attribute.CODE }[0]
            if (codeAttribute !is CodeAttribute) {
                throw AssertionError("CodeAttribute for $methodName.$methodDescriptor not found")
            }
            partialEvaluator.visitCodeAttribute(clazz, method, codeAttribute)
            return codeAttribute
        }

        fun collectInvocationsWithStack(clazz: ProgramClass, partialEvaluator: PartialEvaluator, codeAttribute: CodeAttribute): HashMap<Int, MethodWithStack> {

            val ret = HashMap<Int, MethodWithStack>()

            var offset = 0
            while (offset < codeAttribute.u4codeLength) {
                val instruction = InstructionFactory.create(codeAttribute.code, offset)
                if (instruction is ConstantInstruction) {
                    val constant = clazz.getConstant(instruction.constantIndex)
                    if (constant is MethodrefConstant) {
                        val className = constant.getClassName(clazz)
                        val name = constant.getName(clazz)
                        val descriptor = constant.getType(clazz)
                        val signature = "$className.$name.$descriptor"

                        val isStatic = (instruction.opcode == Instruction.OP_INVOKESTATIC || instruction.opcode == Instruction.OP_INVOKEDYNAMIC)

                        val parameterCount: Int = ClassUtil.internalMethodParameterCount(constant.getType(clazz), isStatic)
                        val stackBefore = partialEvaluator.getStackBefore(offset)

                        val stack = ArrayList<Value>()

                        for (i in 0 until parameterCount) {
                            stack.add(stackBefore.getTop(i))
                        }
                        ret[offset] = MethodWithStack(signature, stack)
                    }
                }
                offset += instruction.length(offset)
            }
            return ret
        }

        fun printDebug(collectInvocationsWithStack: HashMap<Int, MethodWithStack>) {
            val keys = collectInvocationsWithStack.keys.toMutableList()
            keys.sort()
            for (offset in keys) {
                val methodWithStack = collectInvocationsWithStack[offset]!!

                println("[$offset]\t${methodWithStack.methodSignature}")
                for (s in methodWithStack.stack) {
                    println(
                        "\t" + s.javaClass.simpleName +
                            "\t" + (if (s is ParticularReferenceValue) s.type else "") +
                            "\t" + (if (s is ParticularReferenceValue) s.value()?.toString() else "")
                    )
                }
            }
        }
    }
}

data class MethodWithStack(
    var methodSignature: String,
    var stack: ArrayList<Value> = ArrayList()
)
