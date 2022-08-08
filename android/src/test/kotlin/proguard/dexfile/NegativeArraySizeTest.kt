package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.and
import proguard.testutils.match

class NegativeArraySizeTest : FreeSpec({
    "Negative array size test" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "negative-array-size.smali",
                """
                    .class Li;
                    .super Ljava/lang/Object;

                    .method public getFileLength()I
                      .catch Ljava/lang/Exception; { :L0 .. :L1 } :L2
                      .catch Ljava/lang/Exception; { :L3 .. :L4 } :L5
                      .registers 3
                        const/4 v0, -1
                      :L0
                        new-array v1, v0, [I
                      :L1
                        goto :L0
                      :L2
                        move-exception v0
                        const/4 v0, 0
                        sput v0, Lz;->b:I
                      :L3
                        iget v0, p0, Lz;->b:I
                      :L4
                        return v0
                      :L5
                        move-exception v0
                        throw v0
                    .end method
                """.trimIndent()
            )
        )
        val testClass = programClassPool.getClass("i")
        val testMethod = testClass.findMethod("getFileLength", "()I")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class i" {
            testClass shouldNotBe null
        }

        "Check if class i has method getFileLength" {
            testMethod shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            with(testClass and testMethod) {
                match {
                    iconst_m1()
                    newarray(10)
                    astore(1)
                    goto_(-4)
                    astore(1)
                    iconst_0()
                    putstatic("z", "b", "I")
                    aload(0)
                    getfield("z", "b", "I")
                    istore(2)
                    iload(2)
                    ireturn()
                    astore(1)
                    aload(1)
                    athrow()
                } shouldBe true
            }
        }
    }
})
