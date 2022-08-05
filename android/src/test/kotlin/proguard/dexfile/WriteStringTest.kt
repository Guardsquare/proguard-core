package proguard.dexfile

import SmaliSource
import fromSmali
import getSmaliResource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import testutils.ClassPoolBuilder

class WriteStringTest : FreeSpec({
    "Write string test" - {
        val smaliFile = getSmaliResource("writeString.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(SmaliSource(smaliFile.name, smaliFile.readText()))

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
