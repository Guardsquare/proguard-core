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

class MLTest : FreeSpec({
    "ML test" - {
        val smaliFile = getSmaliResource("ML.smali")
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))

        val testClass = programClassPool.getClass("ML")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool has class ML" {
            testClass shouldNotBe null
        }

        "Check if class ML has a constructor" {
            testClass
                .findMethod("<init>", "()V") shouldNotBe null
        }

        "Check if class ML has method a" {
            testClass
                .findMethod("a", "()Ljava/lang/Object;") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .aload(0)
                .invokespecial("java/lang/Object", "<init>", "()V")
                .return_()
                .iconst(3)
                .newarray(10)
                .astore(1)
                .aload(1)
                .iconst(0)
                .iconst(4)
                .iastore()
                .aload(1)
                .iconst(1)
                .iconst(5)
                .iastore()
                .aload(1)
                .iconst(2)
                .bipush(6)
                .iastore()
                .ldc(libraryClassPool.getClass("java/lang/String"))
                .aload(1)
                .invokestatic("java/lang/reflect/Array", "newInstance", "(Ljava/lang/Class;[I)Ljava/lang/Object;")
                .checkcast("[[[Ljava/lang/String;")
                .areturn()

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
