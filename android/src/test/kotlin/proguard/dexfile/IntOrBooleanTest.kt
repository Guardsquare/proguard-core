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

class IntOrBooleanTest : FreeSpec({
    "Int or Boolean test" - {
        val smali = getSmaliResource("int-or-boolean.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                smali.name,
                smali.readText()
            )
        )

        val testClass = programClassPool.getClass("i/or/Z")
        val testMethod = testClass.findMethod(
            "access\$376",
            "(Lcom/google/android/finsky/widget/consumption/NowPlayingWidgetProvider\$ViewTreeWrapper;I)Z"
        )

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class i/or/Z" {
            testClass shouldNotBe null
        }

        "Check if class has method access$376" - {
            testMethod shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            with(testClass and testMethod) {
                match {
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
                } shouldBe true
            }
        }
    }
})
