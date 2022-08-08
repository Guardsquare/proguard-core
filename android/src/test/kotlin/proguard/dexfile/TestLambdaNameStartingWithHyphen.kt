package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.testutils.ClassPoolBuilder

class TestLambdaNameStartingWithHyphen : FreeSpec({

    "Given a small Smali file with a lambda expression starting with a hyphen" - {

        "When the class is loaded into the class pool" - {

            val (pcp, _) = ClassPoolBuilder.fromSmali(
                SmaliSource(
                    "lambdaStartingWithHyphen.smali",
                    """
                        .class public final L-${'$'}${'$'}Lambda${'$'}RetrofitProvider${'$'}7UGyImjn5OERU8TG-W_Zn0fdFtY;
                        .super Ljava/lang/Object;
                    """.trimIndent()
                )
            )

            "Then the lambda class name should not be altered and still have the hyphens in the name" {
                val className = "-${'$'}${'$'}Lambda${'$'}RetrofitProvider${'$'}7UGyImjn5OERU8TG-W_Zn0fdFtY"
                pcp.getClass(className) shouldNotBe null
            }
        }
    }
})
