package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.android.testutils.getSmaliResource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.and
import proguard.testutils.match

class NegativeArraySizeTest : FreeSpec({
    "Negative array size test" - {
        val smali = getSmaliResource("negative-array-size.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                smali.name,
                smali.readText()
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
