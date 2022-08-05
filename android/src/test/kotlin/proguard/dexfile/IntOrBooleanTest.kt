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

class IntOrBooleanTest : FreeSpec({
    "Int or Boolean test" - {
        val smaliFile = getSmaliResource("int-or-boolean.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))
//        programClassPool.classesAccept(ClassPrinter())

        val testClass = programClassPool.getClass("i/or/Z")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class i/or/Z" {
            testClass shouldNotBe null
        }

        "Check if class has method access$376" - {
            testClass
                .findMethod("access\$376", "(Lcom/google/android/finsky/widget/consumption/NowPlayingWidgetProvider\$ViewTreeWrapper;I)Z") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .aload_0()
                .getfield("com/google/android/finsky/widget/consumption/NowPlayingWidgetProvider\$ViewTreeWrapper", "showBackground", "Z")
                .iload_1()
                .ior()
                .i2b()
                .istore_2()
                .aload_0()
                .iload_2()
                .putfield("com/google/android/finsky/widget/consumption/NowPlayingWidgetProvider\$ViewTreeWrapper", "showBackground", "Z")
                .iload_2()
                .ireturn()

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
