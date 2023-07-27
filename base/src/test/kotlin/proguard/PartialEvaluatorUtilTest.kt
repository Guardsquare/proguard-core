package proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.LocalVariableTableAttribute
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.editor.AttributesEditor
import proguard.classfile.editor.ConstantPoolEditor
import proguard.classfile.instruction.Instruction
import proguard.classfile.visitor.MemberVisitor
import proguard.evaluation.BasicInvocationUnit
import proguard.evaluation.PartialEvaluator
import proguard.evaluation.value.TypedReferenceValueFactory
import proguard.testutils.AssemblerSource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.PartialEvaluatorUtil

class PartialEvaluatorUtilTest : FreeSpec({
    "Test removing LocalVariableTableAttribute" {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            AssemblerSource(
                "Main.jbc",
                """
                version 1.8;
                public class Main extends java.lang.Object [
                    SourceFile "Main.java";
                ] {
                    public void test() {
                        line 7
                            bipush 26
                            istore_0
                            return
                    }
                }
                """.trimIndent()
            )
        )

        programClassPool.classAccept("Main") {
            it.methodAccept(
                "test",
                "()V",
                object : MemberVisitor {
                    override fun visitProgramMethod(programClass: ProgramClass, programMethod: ProgramMethod) {
                        programMethod.attributesAccept(
                            programClass,
                            object : AttributeVisitor {
                                override fun visitCodeAttribute(clazz: Clazz, method: Method, codeAttribute: CodeAttribute) {
                                    val constantPoolEditor = ConstantPoolEditor(programClass)
                                    val index = constantPoolEditor.addUtf8Constant(Attribute.LOCAL_VARIABLE_TABLE)

                                    val attributesEditor = AttributesEditor(programClass, programMethod, codeAttribute, false)
                                    attributesEditor.addAttribute(LocalVariableTableAttribute(index, 0, arrayOf()))
                                    attributesEditor.deleteAttribute(Attribute.LOCAL_VARIABLE_TABLE)
                                }
                            }
                        )
                    }
                }
            )
        }

        val valueFactory = TypedReferenceValueFactory()
        val invocationUnit = BasicInvocationUnit(valueFactory)
        val partialEvaluator = PartialEvaluator(valueFactory, invocationUnit, true)
        val (instructions, _) = PartialEvaluatorUtil.evaluate(
            "Main",
            "test",
            "()V",
            programClassPool,
            partialEvaluator
        )
        instructions.map { it.second.opcode } shouldBe arrayOf(
            Instruction.OP_BIPUSH,
            Instruction.OP_ISTORE_0,
            Instruction.OP_RETURN
        )
    }
})
