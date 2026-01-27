package proguard.testutils.classfile.visitor

import proguard.classfile.AccessConstants
import proguard.classfile.ClassPool
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.constant.AnyMethodrefConstant
import proguard.classfile.constant.Constant
import proguard.classfile.constant.visitor.ConstantVisitor
import proguard.classfile.editor.InstructionSequenceBuilder
import proguard.classfile.instruction.ConstantInstruction
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.classfile.util.ClassUtil
import proguard.testutils.classfile.ClassBuilder

/**
 * This InstructionVisitor creates stub classes/methods for any method call it visits and adds the stubs to the given class pool.
 */
class LibraryClassStubGenerator(val classPool: ClassPool) : InstructionVisitor, ConstantVisitor {

    private val invokeOps = setOf(
        Instruction.OP_INVOKESTATIC,
        Instruction.OP_INVOKEVIRTUAL,
        Instruction.OP_INVOKESPECIAL,
        Instruction.OP_INVOKEINTERFACE,
    )

    private var isStatic = false
    private var isInterface = false

    override fun visitAnyInstruction(
        clazz: Clazz?,
        method: Method?,
        codeAttribute: CodeAttribute?,
        offset: Int,
        instruction: Instruction?,
    ) {
    }

    override fun visitConstantInstruction(
        clazz: Clazz,
        method: Method,
        codeAttribute: CodeAttribute,
        offset: Int,
        constantInstruction: ConstantInstruction,
    ) {
        isStatic = constantInstruction.opcode == Instruction.OP_INVOKESTATIC
        isInterface = constantInstruction.opcode == Instruction.OP_INVOKEINTERFACE

        if (invokeOps.contains(constantInstruction.opcode)) {
            clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this)
        }
    }

    override fun visitAnyMethodrefConstant(
        clazz: Clazz,
        anyMethodrefConstant: AnyMethodrefConstant,
    ) {
        val className = anyMethodrefConstant.getClassName(clazz)
        val methodName = anyMethodrefConstant.getName(clazz)
        val methodType = anyMethodrefConstant.getType(clazz)
        val calledClass = (
            classPool.getClass(className) ?: {
                val clz = ClassBuilder.Companion.createClass(className)
                classPool.addClass(clz)
                clz
            }()
            ) as ProgramClass
        val calledMethod = calledClass.findMethod(methodName, methodType) ?: ClassBuilder.Companion.addMethod(
            calledClass,
            AccessConstants.PUBLIC or (if (isStatic) AccessConstants.STATIC else 0),
            methodName,
            methodType,
            dummyForDescriptor(methodType),
        )
    }

    override fun visitAnyConstant(clazz: Clazz?, constant: Constant?) {}

    fun dummyForDescriptor(descriptor: String): Array<Instruction> {
        val isb = InstructionSequenceBuilder()
        when (ClassUtil.internalMethodReturnType(descriptor)) {
            "V" -> isb.return_()
            "I", "Z", "B", "C", "S" -> isb.pushInt(0).ireturn()
            "J" -> isb.pushLong(0).lreturn()
            "F" -> isb.pushFloat(0F).freturn()
            "D" -> isb.pushDouble(0.0).dreturn()
            else -> isb.aconst_null().areturn()
        }
        return isb.instructions()
    }
}
