package proguard.dexfile

import SmaliSource
import fromSmali
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import testutils.ClassPoolBuilder

class LoopEnclosingClassTest : FreeSpec({
    "Loop enclosing class test" - {
        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "a.smali",
                """
                .class La;
                .super Ljava/lang/Object;

                # annotations
                .annotation system Ldalvik/annotation/EnclosingClass;
                    value = La;
                .end annotation

                """.trimIndent()
            ),
            SmaliSource(
                "b.smali",
                """
                .class Lb;
                .super Ljava/lang/Object;
                
                # annotations
                .annotation system Ldalvik/annotation/EnclosingClass;
                    value = Lc;
                .end annotation

                """.trimIndent()
            ),
            SmaliSource(
                "c.smali",
                """
                .class Lc;
                .super Ljava/lang/Object;
                
                # annotations
                .annotation system Ldalvik/annotation/EnclosingClass;
                    value = Lb;
                .end annotation

                """.trimIndent()
            )
        )

        "Check if classPool is not null" {
            programClassPool shouldNotBe null
        }

        "Check if classPool has class a" {
            programClassPool.getClass("a") shouldNotBe null
        }

        "Check if classPool has class b" {
            programClassPool.getClass("b") shouldNotBe null
        }

        "Check if classPool has class c" {
            programClassPool.getClass("c") shouldNotBe null
        }
    }
})
