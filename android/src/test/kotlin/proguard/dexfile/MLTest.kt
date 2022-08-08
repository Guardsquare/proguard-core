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

class MLTest : FreeSpec({
    "ML test" - {
        val smali = getSmaliResource("ML.smali")
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                smali.name,
                smali.readText()
            )
        )

        val testClass = programClassPool.getClass("ML")
        val methodInit = testClass.findMethod("<init>", "()V")
        val methodA = testClass.findMethod("a", "()Ljava/lang/Object;")

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool has class ML" {
            testClass shouldNotBe null
        }

        "Check if class ML has a constructor" {
            methodInit shouldNotBe null
        }

        "Check if class ML has method a" {
            methodA shouldNotBe null
        }

        "Check if sequence of operations for method <init> after translation match original smali code" {
            with(testClass and methodInit) {
                match {
                    aload(0)
                    invokespecial("java/lang/Object", "<init>", "()V")
                    return_()
                } shouldBe true
            }
        }

        "Check if sequence of operations for method a after translation match original smali code" {
            with(testClass and methodA) {
                match {
                    iconst(3)
                    newarray(10)
                    astore(1)
                    aload(1)
                    iconst(0)
                    iconst(4)
                    iastore()
                    aload(1)
                    iconst(1)
                    iconst(5)
                    iastore()
                    aload(1)
                    iconst(2)
                    bipush(6)
                    iastore()
                    ldc(libraryClassPool.getClass("java/lang/String"))
                    aload(1)
                    invokestatic("java/lang/reflect/Array", "newInstance", "(Ljava/lang/Class;[I)Ljava/lang/Object;")
                    checkcast("[[[Ljava/lang/String;")
                    areturn()
                } shouldBe true
            }
        }
    }
})
