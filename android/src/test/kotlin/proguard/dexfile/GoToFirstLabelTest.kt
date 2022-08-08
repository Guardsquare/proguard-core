package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.android.testutils.getSmaliResource
import proguard.testutils.ClassPoolBuilder

class GoToFirstLabelTest : FreeSpec({
    "Go to first label test" - {
        val smali = getSmaliResource("goto-first-label.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                smali.name,
                smali.readText()
            )
        )
        val testClass = programClassPool.getClass("xgoto/first/label")
        val methodAssertSlept = testClass.findMethod("assertSlept", "()V")
        val methodG2 = testClass.findMethod("g2", "(LObj;)V")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class xgoto/first/label" {
            testClass shouldNotBe null
        }

        "Check if class has method assertSlept" {
            methodAssertSlept shouldNotBe null
        }

        "Check if class has method g2" {
            methodG2 shouldNotBe null
        }
    }
})
