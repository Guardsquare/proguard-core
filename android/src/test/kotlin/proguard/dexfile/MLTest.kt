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

class MLTest : FreeSpec({
    "ML test" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "ML.smali",
                """
                    .class LML;
                    .super Ljava/lang/Object;
                    .source "ML.java"


                    # direct methods
                    .method constructor <init>()V
                        .registers 1

                        .prologue
                        .line 1
                        invoke-direct {p0}, Ljava/lang/Object;-><init>()V

                        return-void
                    .end method


                    # virtual methods
                    .method a()Ljava/lang/Object;
                        .registers 4

                        .prologue
                        .line 3
                        const/4 v0, 0x4

                        const/4 v1, 0x5

                        const/4 v2, 0x6

                        filled-new-array {v0, v1, v2}, [I

                        move-result-object v0

                        const-class v1, Ljava/lang/String;

                        invoke-static {v1, v0}, Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;

                        move-result-object v0

                        check-cast v0, [[[Ljava/lang/String;

                        return-object v0
                    .end method
                """.trimIndent()
            )
        )

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
            val instructionBuilder = with(InstructionBuilder()) {
                aload(0)
                invokespecial("java/lang/Object", "<init>", "()V")
                return_()
                iconst(3)
                newarray(10)
                astore(1)
                aload(1)
                iconst(0)
                iconst(4)
                iastore()
                aload(1)
                iconst(1)
                iconst(5)
                iastore()
                aload(1)
                iconst(2)
                bipush(6)
                iastore()
                ldc(libraryClassPool.getClass("java/lang/String"))
                aload(1)
                invokestatic("java/lang/reflect/Array", "newInstance", "(Ljava/lang/Class;[I)Ljava/lang/Object;")
                checkcast("[[[Ljava/lang/String;")
                areturn()
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
