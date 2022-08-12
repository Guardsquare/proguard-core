package com.guardsquare.proguard.tools

import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramField
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.visitor.AttributeConstantVisitor
import proguard.classfile.constant.Constant
import proguard.classfile.constant.StringConstant
import proguard.classfile.constant.visitor.ConstantVisitor
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.instruction.visitor.InstructionConstantVisitor
import proguard.classfile.util.ClassUtil
import proguard.classfile.visitor.AllMemberVisitor
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MemberVisitor

class StringPrinter :
    ClassVisitor,
    MemberVisitor,
    ConstantVisitor {
    private val strings: MutableSet<String> = HashSet()
    override fun visitAnyClass(clazz: Clazz) {}
    override fun visitProgramClass(programClass: ProgramClass) {
        programClass.accept(AllMemberVisitor(this))
    }

    override fun visitAnyMember(clazz: Clazz, member: Member) {}
    override fun visitProgramField(programClass: ProgramClass, programField: ProgramField) {
        strings.clear()
        programField.attributesAccept(programClass, AttributeConstantVisitor(this))
        if (strings.isNotEmpty()) {
            val name = ClassUtil.externalClassName(programClass.name) + " " +
                ClassUtil.externalFullFieldDescription(
                    programField.u2accessFlags,
                    programField.getName(programClass),
                    programField.getDescriptor(programClass)
                )
            println(name)
            for (s in strings) println("    $s")
        }
    }

    override fun visitProgramMethod(programClass: ProgramClass, programMethod: ProgramMethod) {
        strings.clear()
        programMethod.attributesAccept(
            programClass,
            AllInstructionVisitor(
                InstructionConstantVisitor(this)
            )
        )
        if (strings.isNotEmpty()) {
            val name = ClassUtil.externalClassName(programClass.name) + " " +
                ClassUtil.externalFullMethodDescription(
                    programClass.name,
                    programMethod.u2accessFlags,
                    programMethod.getName(programClass),
                    programMethod.getDescriptor(programClass)
                )
            println(name)
            for (s in strings) println("    $s")
        }
    }

    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}
    override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
        strings.add(stringConstant.getString(clazz))
    }
}
