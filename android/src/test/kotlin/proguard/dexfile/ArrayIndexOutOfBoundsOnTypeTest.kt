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

class ArrayIndexOutOfBoundsOnTypeTest : FreeSpec({
    "Array index out of bounds on type test" - {
        val smaliFile = getSmaliResource("bb-5-ArrayIndexOutOfBoundsOnType.smali")
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))
//        programClassPool.classesAccept(ClassPrinter())

        val testClass = programClassPool.getClass("i")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class i" {
            testClass shouldNotBe null
        }

        "Check if classs i has method a" {
            testClass
                .findMethod("a", "(Ljava/lang/Object;IZ)Ljava/lang/Object;") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .iconst(0)
                .istore(5)
                .iconst(0)
                .istore(6)
                .aload(1)
                .instanceof_(libraryClassPool.getClass("java/lang/Byte"))
                .ifeq(16)
                .aload(0)
                .iconst(0)
                .iload(2)
                .iload(3)
                .invokevirtual("ct/be", "a", "(BIZ)B")
                .invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;")
                .astore(1)
                .aload(1)
                .areturn()
                .aload(1)
                .instanceof_(libraryClassPool.getClass("java/lang/Boolean"))
                .ifeq(25)
                .aload(0)
                .iconst(0)
                .iload(2)
                .iload(3)
                .invokevirtual("ct/be", "a", "(BIZ)B")

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
