package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.android.testutils.getSmaliResource
import proguard.testutils.ClassPoolBuilder

class ArrayIndexOutOfBoundsOnTypeTest : FreeSpec({
    "Array index out of bounds on type test" - {
        val smali = getSmaliResource("bb-5-ArrayIndexOutOfBoundsOnType.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(smali.name, smali.readText())
        )
        val testClass = programClassPool.getClass("i")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class i" {
            testClass shouldNotBe null
        }

        "Check if classs i has method a" {
            testClass
                .findMethod("a", "(Ljava/lang/Object;IZ)Ljava/lang/Object;") shouldNotBe null
        }
    }
})
