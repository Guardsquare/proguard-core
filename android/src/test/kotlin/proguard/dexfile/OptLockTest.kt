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

class OptLockTest : FreeSpec({
    "Opt lock test" - {
        val smaliFile = getSmaliResource("opt-lock.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))

        val testClass = programClassPool.getClass("opt/lock")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class opt/lock" {
            testClass shouldNotBe null
        }

        "Check if class has method a" {
            testClass.findMethod("a", "()V") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                .astore(0)
                .aload(0)
                .monitorenter()
                .aload(0)
                .ldc("haha")
                .invokevirtual("java/io/PrintString", "println", "(Ljava/lang/String;)V")
                .aload(0)
                .monitorexit()
                .return_()
                .astore(1)
                .aload(0)
                .monitorexit()
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
