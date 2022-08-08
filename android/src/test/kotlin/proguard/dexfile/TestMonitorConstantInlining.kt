package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.android.testutils.getSmaliResource
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.runClassPool

class TestMonitorConstantInlining : FreeSpec({

    "Given a smali file" - {

        "When loading and translating the smali file" - {
            val smali = getSmaliResource("TestMonitorConstantInlining2297.smali")
            val (pcp, _) = ClassPoolBuilder.fromSmali(
                SmaliSource(
                    smali.name,
                    smali.readText()
                )
            )

            "Then the resulting classpool should not be null" {
                pcp shouldNotBe null
            }

            "Then the resulting classpool should contain the TestMonitorConstantInlining2297 class" {
                pcp.getClass("TestMonitorConstantInlining2297") shouldNotBe null
            }

            "Then the output after running should be correct" {
                val expected = "The answer is 42\n"
                val output = runClassPool(pcp, "TestMonitorConstantInlining2297", arrayOf())

                output shouldBe expected
            }
        }
    }
})
