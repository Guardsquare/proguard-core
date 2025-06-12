package proguard.classfile.editor

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.VersionConstants
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.ExceptionInfo
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.InstructionFactory
import proguard.classfile.instruction.SimpleInstruction

class CodeAttributeComposerTest : FunSpec({
    test("CodeAttributeComposer should allow code beyond its maxmimum fragment size") {
        val codeComposer = CodeAttributeComposer()
        codeComposer.reset()
        codeComposer.beginCodeFragment(0)
        codeComposer.appendInstruction(ClassEstimates.TYPICAL_CODE_LENGTH + 1, InstructionFactory.create(Instruction.OP_NOP))
        codeComposer.endCodeFragment()
    }

    test("Adding an exception at the beginning of the table") {
        val codeComposer = CodeAttributeComposer()

        codeComposer.reset()
        codeComposer.beginCodeFragment(50)

        val tryBegin = 0
        val tryEnd = 1
        val catch = 2

        codeComposer.appendLabel(tryBegin)
        codeComposer.appendInstruction(SimpleInstruction(Instruction.OP_ICONST_0))
        codeComposer.appendLabel(tryEnd)
        codeComposer.appendInstruction(SimpleInstruction(Instruction.OP_RETURN))
        codeComposer.appendLabel(catch)
        codeComposer.appendInstruction(SimpleInstruction(Instruction.OP_ATHROW))

        val exception0 = ExceptionInfo(tryBegin, tryEnd, catch, 0)
        codeComposer.appendException(exception0)
        val exception1 = ExceptionInfo(tryEnd, catch, catch, 0)
        codeComposer.addException(exception1, 0)

        codeComposer.endCodeFragment()

        // Create an empty class.
        val programClass =
            ProgramClass(
                VersionConstants.CLASS_VERSION_1_8,
                1,
                arrayOfNulls(10),
                AccessConstants.PUBLIC,
                0,
                0,
            )
        val constantPoolEditor = ConstantPoolEditor(programClass)
        val programMethod =
            ProgramMethod(
                AccessConstants.PUBLIC,
                constantPoolEditor.addUtf8Constant("foo"),
                constantPoolEditor.addUtf8Constant("()V"),
                null,
            )

        codeComposer.addCodeAttribute(programClass, programMethod)

        programMethod.attributesAccept(
            programClass,
            object : AttributeVisitor {
                override fun visitAnyAttribute(clazz: Clazz?, attribute: Attribute?) {
                }

                override fun visitCodeAttribute(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?) {
                    codeAttribute!!.exceptionTable.size shouldBe 2
                    codeAttribute.exceptionTable[0] shouldBe exception1
                }
            },
        )
    }
})
