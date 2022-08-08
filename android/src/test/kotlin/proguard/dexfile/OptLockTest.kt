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

class OptLockTest : FreeSpec({
    "Opt lock test" - {
        val smali = getSmaliResource("opt-lock.smali")
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                smali.name,
                smali.readText()
            )
        )

        val testClass = programClassPool.getClass("opt/lock")
        val testMethod = testClass.findMethod("a", "()V")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class opt/lock" {
            testClass shouldNotBe null
        }

        "Check if class has method a" {
            testMethod shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            with(testClass and testMethod) {
                match {
                    getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                    astore(0)
                    aload(0)
                    monitorenter()
                    aload(0)
                    ldc("haha")
                    invokevirtual("java/io/PrintString", "println", "(Ljava/lang/String;)V")
                    aload(0)
                    monitorexit()
                    return_()
                    astore(1)
                    aload(0)
                    monitorexit()
                    aload(1)
                    athrow()
                } shouldBe true
            }
        }
    }
})
