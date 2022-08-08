package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.util.InstructionSequenceMatcher
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.InstructionBuilder

class IntOrBooleanTest : FreeSpec({
    "Int or Boolean test" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "int-or-boolean.smali",
                """
                    .class Li/or/Z;
                    .super Ljava/lang/Object;

                    .method static synthetic access${'$'}376(Lcom/google/android/finsky/widget/consumption/NowPlayingWidgetProvider${'$'}ViewTreeWrapper;I)Z
                        .registers 3
                        iget-boolean v0, v1, Lcom/google/android/finsky/widget/consumption/NowPlayingWidgetProvider${'$'}ViewTreeWrapper;->showBackground:Z
                        or-int/2addr v0, v2
                        int-to-byte v0, v0
                        iput-boolean v0, v1, Lcom/google/android/finsky/widget/consumption/NowPlayingWidgetProvider${'$'}ViewTreeWrapper;->showBackground:Z
                        return v0
                    .end method

                """.trimIndent()
            )
        )

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
            val instructionBuilder = with(InstructionBuilder()) {
                aload_0()
                getfield("com/google/android/finsky/widget/consumption/NowPlayingWidgetProvider\$ViewTreeWrapper", "showBackground", "Z")
                iload_1()
                ior()
                i2b()
                istore_2()
                aload_0()
                iload_2()
                putfield("com/google/android/finsky/widget/consumption/NowPlayingWidgetProvider\$ViewTreeWrapper", "showBackground", "Z")
                iload_2()
                ireturn()
            }

            val matcher = InstructionSequenceMatcher(instructionBuilder.constants(), instructionBuilder.instructions())

            testClass.methodsAccept(
                AllAttributeVisitor(
                    AllInstructionVisitor(matcher)
                )
            )

            matcher.isMatching shouldBe true
        }
    }
})
