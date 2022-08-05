package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.util.InstructionSequenceMatcher
import testutils.ClassPoolBuilder
import testutils.InstructionBuilder

class OptLockTest : FreeSpec({
    "Opt lock test" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "opt-lock.smali",
                """
                    .class Lopt/lock;
                    .super Ljava/lang/Object;
                    .method public static a()V
                        .catchall { :L0 .. :L1 } :L2
                        .registers 2
                        sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
                        :L0
                        monitor-enter v0
                        const-string v1, "haha"
                        invoke-virtual { v0, v1 }, Ljava/io/PrintString;->println(Ljava/lang/String;)V
                        :L1
                        monitor-exit v0
                        return-void
                        :L2
                        move-exception v1
                        monitor-exit v0
                        throw v1
                    .end method
                """.trimIndent()
            )
        )

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
            val instructionBuilder = with(InstructionBuilder()) {
                getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                astore(0)
                aload(0)
                monitorenter()
                aload(0)
                ldc("haha")
                invokevirtual("java/io/PrintString", "println", "(Ljava/lang/String;)V")
                aload(0)
                monitorexit()
                return_()
                astore(1)
                aload(0)
                monitorexit()
                aload(1)
                athrow()
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
