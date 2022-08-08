package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.and
import proguard.testutils.match

class MLTest : FreeSpec({
    "ML test" - {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "ML.smali",
                """
                    .class LML;
                    .super Ljava/lang/Object;
                    .source "ML.java"


                    # direct methods
                    .method constructor <init>()V
                        .registers 1

                        .prologue
                        .line 1
                        invoke-direct {p0}, Ljava/lang/Object;-><init>()V

                        return-void
                    .end method


                    # virtual methods
                    .method a()Ljava/lang/Object;
                        .registers 4

                        .prologue
                        .line 3
                        const/4 v0, 0x4

                        const/4 v1, 0x5

                        const/4 v2, 0x6

                        filled-new-array {v0, v1, v2}, [I

                        move-result-object v0

                        const-class v1, Ljava/lang/String;

                        invoke-static {v1, v0}, Ljava/lang/reflect/Array;->newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;

                        move-result-object v0

                        check-cast v0, [[[Ljava/lang/String;

                        return-object v0
                    .end method
                """.trimIndent()
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
