package proguard.testutils.classfile.runner

import proguard.classfile.AccessConstants
import proguard.classfile.ClassPool
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.instruction.Instruction
import proguard.classfile.visitor.MethodCreator
import proguard.testutils.classfile.CodeAttributeBuilder
import java.lang.reflect.Method
import kotlin.text.iterator

class InstructionSequenceRunner(instructions: Array<Instruction>, name: String, descriptor: String, pgClazz: ProgramClass) {

    lateinit var method: Method
    lateinit var instance: Any

    init {
        val programClassPool = ClassPool()
        programClassPool.addClass(pgClazz)
        pgClazz.accept(MethodCreator(programClassPool, ClassPool(), name, descriptor, AccessConstants.PUBLIC, 0, false))
        val pgMethod = pgClazz.findMethod(name, descriptor) as ProgramMethod
        replaceCodeAttribute(pgMethod, CodeAttributeBuilder.Companion.fromInstructionArray(pgClazz, pgMethod, instructions))

        val loader = ClassPoolClassLoader(programClassPool)
        val clazz = Class.forName("placeholder", true, loader)
        instance = clazz.getConstructor().newInstance()
        method = clazz.getDeclaredMethod(name, *transformDescriptor(descriptor))
    }

    fun invoke(vararg args: Any): Any {
        return method.invoke(instance, *args)
    }

    private fun transformDescriptor(descriptor: String): Array<Class<Any>> {
        val result = mutableListOf<Class<Any>>()
        for (char in descriptor) {
            if (char == '(') continue
            if (char == ')') break
            result.add(
                when (char) {
                    'I' -> Int.javaClass
                    'J' -> Long.javaClass
                    'B' -> Byte.javaClass
                    'C' -> Char.javaClass
                    'S' -> Short.javaClass
                    'Z' -> Boolean.javaClass
                    'F' -> Float.javaClass
                    'D' -> Double.javaClass
                    else -> throw Exception("Unsupported type: $char")
                },
            )
        }
        return result.toTypedArray()
    }

    private fun replaceCodeAttribute(method: ProgramMethod, codeAttribute: CodeAttribute) {
        for (i in 0 until method.attributes.size) {
            if (method.attributes[i] is CodeAttribute) {
                method.attributes[i] = codeAttribute
            }
        }
    }
}
