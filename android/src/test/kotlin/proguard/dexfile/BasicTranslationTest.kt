package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.and
import proguard.testutils.match

class BasicTranslationTest : FreeSpec({

    "Basic Hello World translation test" - {

        val (programClassPool, _) = ClassPoolBuilder.fromSmali(
            SmaliSource(
                "HelloWorld.smali",
                """
                .class public LHelloWorld;
                
                .super Ljava/lang/Object;
                
                .method public static main([Ljava/lang/String;)V
                    .registers 2
                    
                    sget-object     v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
                    
                    const-string	v1, "Hello World!"
                    
                    invoke-virtual  {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                    
                    return-void
                .end method
                """.trimIndent()
            )
        )

        val helloWorldClass = programClassPool.getClass("HelloWorld")
        val mainMethod = helloWorldClass.findMethod("main", "([Ljava/lang/String;)V")

        "Check if classPool contains the HelloWorld class" {
            helloWorldClass shouldNotBe null
        }

        "Check if HelloWorld contains main method" {
            mainMethod shouldNotBe null
        }

        "Check if sequence of operations after translation match original smali code" {
            with(helloWorldClass and mainMethod) {
                match {
                    getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                    ldc("Hello World!")
                    invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                    return_()
                } shouldBe true
            }
        }
    }
})
