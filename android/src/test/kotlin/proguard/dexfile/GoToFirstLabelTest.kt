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

class GoToFirstLabelTest : FreeSpec({
    "Go to first label test" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "goto-first-label.smali",
                """
                    .class Lxgoto/first/label;
                    .super Ljava/lang/Object;

                    .method public static assertSlept()V
                        .registers 3
                        :L0
                        sget-object v1, LA;->sleepSemaphore:Ljava/util/concurrent/Semaphore;
                        invoke-virtual { v1 }, Ljava/util/concurrent/Semaphore;->availablePermits()I
                        move-result v1
                        if-nez v1, :L1
                        return-void
                        :L1
                        const-wide/16 v1, 50
                        invoke-static { v1, v2 }, Ljava/lang/Thread;->sleep(J)V
                        goto :L0
                    .end method

                    .method public static g2(LObj;)V
                        .registers 4
                        :L0
                        invoke-virtual { p0 }, LObj;->next()LObj;
                        move-result-object v3
                        if-nez v3, :L1
                        return-void
                        :L1
                        invoke-virtual { p0 }, LObj;->next()LObj;
                        move-result-object p0
                        goto :L0
                    .end method
                """.trimIndent()
            )
        )
        val testClass = programClassPool.getClass("xgoto/first/label")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class xgoto/first/label" {
            testClass shouldNotBe null
        }

        "Check if class has method assertSlept" {
            testClass
                .findMethod("assertSlept", "()V") shouldNotBe null
        }

        "Check if class has method g2" {
            testClass
                .findMethod("g2", "(LObj;)V") shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            val instructionBuilder = with(InstructionBuilder()) {
                getstatic("A", "sleepSemaphore", "Ljava/util/concurrent/Semaphore;")
                invokevirtual("java/util/concurrent/Semaphore", "availablePermits", "()I")
                ifne(4)
                return_()
                ldc2_w(50L)
                invokestatic("java/lang/Thread", "sleep", "(J)V")
                goto_(-16)
                aload(0)
                invokevirtual("Obj", "next", "()LObj;")
                astore(0)
                aload(0)
                ifnonnull(4)
                return_()
                aload(0)
                invokevirtual("Obj", "next", "()LObj;")
                astore(0)
                goto_(-15)
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
