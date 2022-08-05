package proguard.dexfile

import SmaliSource
import fromSmali
import getSmaliResource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.classfile.util.InstructionSequenceMatcher
import testutils.ClassPoolBuilder
import testutils.InstructionBuilder

class NegativeArraySizeTest : FreeSpec({
    "Negative array size test" - {
        val smaliFile = getSmaliResource("negative-array-size.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))

        val testClass = programClassPool.getClass("i")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class i" {
            testClass shouldNotBe null
        }

        "Check if class i has method getFileLength" {
            testClass.findMethod("getFileLength", "()I") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .iconst_m1()
                .newarray(10)
                .astore(1)
                .goto_(-4)
                .astore(1)
                .iconst_0()
                .putstatic("z", "b", "I")
                .aload(0)
                .getfield("z", "b", "I")
                .istore(2)
                .iload(2)
                .ireturn()
                .astore(1)
                .aload(1)
                .athrow()

            val matcher = InstructionSequenceMatcher(instructionBuilder.constants(), instructionBuilder.instructions())

            // Find the match in the code and print it out.
            class MatchPrinter : InstructionVisitor {
                override fun visitAnyInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, instruction: Instruction) {
                    println(instruction.toString(clazz, offset))
                    instruction.accept(clazz, method, codeAttribute, offset, matcher)
                    if (matcher.isMatching()) {
                        println("  -> matching sequence starting at [" + matcher.matchedInstructionOffset(0) + "]")
                    }
                }
            }

            testClass.methodsAccept(
                AllAttributeVisitor(
                    AllInstructionVisitor(
                        MatchPrinter()
                    )
                )
            )
        }
    }
})
