package proguard.dexfile

import SmaliSource
import fromSmali
import getSmaliResource
// import getSmaliResource
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

class EmptyTryCatchWithGotoHeadTest : FreeSpec({
    "Empty Try Catch With Goto Head test" - {
        val smaliFile = getSmaliResource("empty-try-catch-with-goto-head.smali")
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))


        val testClass = programClassPool.getClass("etcwgh")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class etcwgh" {
            testClass shouldNotBe null
        }

        "Check if class etcwgh contains method aaa" {
            testClass
                .findMethod("aaa", "(F)V") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .aload(0)
                .getfield("z", "z", "Lz;")
                .ifnull(38)
                .aload(0)
                .getfield("z", "z", "Lz;")
                .astore(2)
                .invokestatic("java/util/Locale", "getDefault", "()Ljava/util/Locale;")
                .astore(3)
                .iconst(1)
                .anewarray(libraryClassPool.getClass("java/lang/Object"))
                .astore(4)
                .aload(4)
                .iconst_0()
                .fload(1)
                .invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;")
                .aastore()
                .aload(2)
                .aload(3)
                .ldc("%.1f")
                .aload(4)
                .invokestatic("java/lang/String", "format", "(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")
                .invokevirtual("android/widget/TextView", "setText", "(Ljava/lang/CharSequence;)V")
                .return_()

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
