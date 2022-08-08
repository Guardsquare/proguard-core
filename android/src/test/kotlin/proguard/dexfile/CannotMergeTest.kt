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

class CannotMergeTest : FreeSpec({
    "Can not merge z and i test" - {
        val smali = getSmaliResource("MultiSelectListPreference.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(smali.name, smali.readText())
        )
        val testClass = programClassPool.getClass("android/preference/MultiSelectListPreference")
        val testMethod = testClass.findMethod("test", "(Ljava/util/Set;Ljava/lang/Object;)V")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class android/preference/MultiSelectListPreference" {
            testClass shouldNotBe null
        }

        "Check if the class contains method test" - {
            testMethod shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            with(testClass and testMethod) {
                match {
                    aload(0)
                    aload(1)
                    aload(2)
                    invokeinterface("java/util/Set", "add", "(Ljava/lang/Object;)Z")
                    invokestatic(
                        "android/preference/MultiSelectListPreference", "access$076",
                        "(Landroid/preference/MultiSelectListPreference;I)Z"
                    )
                    pop()
                    return_()
                } shouldBe true
            }
        }
    }
})
