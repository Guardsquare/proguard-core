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

class EmptyTryCatchWithGotoHeadTest : FreeSpec({
    "Empty Try Catch With Goto Head test" - {
        val smali = getSmaliResource("empty-try-catch-with-goto-head.smali")
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(
            SmaliSource(smali.name, smali.readText())
        )
        val testClass = programClassPool.getClass("etcwgh")
        val testMethod = testClass.findMethod("aaa", "(F)V")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool contains class etcwgh" {
            testClass shouldNotBe null
        }

        "Check if class etcwgh contains method aaa" {
            testMethod shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            with(testClass and testMethod) {
                match {
                    aload(0)
                    getfield("z", "z", "Lz;")
                    ifnull(38)
                    aload(0)
                    getfield("z", "z", "Lz;")
                    astore(2)
                    invokestatic("java/util/Locale", "getDefault", "()Ljava/util/Locale;")
                    astore(3)
                    iconst(1)
                    anewarray(libraryClassPool.getClass("java/lang/Object"))
                    astore(4)
                    aload(4)
                    iconst_0()
                    fload(1)
                    invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;")
                    aastore()
                    aload(2)
                    aload(3)
                    ldc("%.1f")
                    aload(4)
                    invokestatic("java/lang/String", "format", "(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")
                    invokevirtual("android/widget/TextView", "setText", "(Ljava/lang/CharSequence;)V")
                    return_()
                } shouldBe true
            }
        }
    }
})
