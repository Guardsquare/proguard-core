package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.android.testutils.getSmaliResource
import proguard.testutils.ClassPoolBuilder

class WriteStringTest : FreeSpec({
    "Write string test" - {
        val smali = getSmaliResource("writeString.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                smali.name,
                smali.readText()
            )
        )
        val testClass = programClassPool.getClass("DD")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool has class DD" {
            testClass shouldNotBe null
        }

        "Check if class ML has method writeString" {
            testClass.findMethod("writeString", "(Ljava/lang/String;[BIZ)I") shouldNotBe null
        }
    }
})
