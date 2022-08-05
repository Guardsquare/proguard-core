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

class EmptyTryCatchWithGotoHeadTest : FreeSpec({
    "Empty Try Catch With Goto Head test" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "empty-try-catch-with-goto-head.smali",
                """
                    .class Letcwgh;
                    .super Ljava/lang/Object;

                    .method private aaa(F)V
                      .catch Ljava/lang/Exception; { :L1 .. :L2 } :L3
                      .registers 8
                      :L0
                        iget-object v0, p0, Lz;->z:Lz;
                        if-eqz v0, :L1
                        iget-object v0, p0, Lz;->z:Lz;
                        invoke-static { }, Ljava/util/Locale;->getDefault()Ljava/util/Locale;
                        move-result-object v1
                        const-string/jumbo v2, "%.1f"
                        const/4 v3, 1
                        new-array v3, v3, [Ljava/lang/Object;
                        const/4 v4, 0
                        invoke-static { p1 }, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;
                        move-result-object v5
                        aput-object v5, v3, v4
                        invoke-static { v1, v2, v3 }, Ljava/lang/String;->format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
                        move-result-object v1
                        invoke-virtual { v0, v1 }, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
                      :L1
                        return-void
                      :L2
                      :L3
                        goto :L0
                    .end method
                """.trimIndent()
            )
        )
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
            val instructionBuilder = with(InstructionBuilder()) {
                aload(0)
                getfield("z", "z", "Lz;")
                ifnull(38)
                aload(0)
                getfield("z", "z", "Lz;")
                astore(2)
                invokestatic("java/util/Locale", "getDefault", "()Ljava/util/Locale;")
                astore(3)
                iconst(1)
                anewarray(libraryClassPool.getClass("java/lang/Object"))
                astore(4)
                aload(4)
                iconst_0()
                fload(1)
                invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;")
                aastore()
                aload(2)
                aload(3)
                ldc("%.1f")
                aload(4)
                invokestatic("java/lang/String", "format", "(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")
                invokevirtual("android/widget/TextView", "setText", "(Ljava/lang/CharSequence;)V")
                return_()
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
