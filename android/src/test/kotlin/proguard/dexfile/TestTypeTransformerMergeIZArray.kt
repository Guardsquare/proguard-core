package proguard.dexfile

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import proguard.android.testutils.SmaliSource
import proguard.android.testutils.fromSmali
import proguard.classfile.editor.CodeAttributeEditor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.and
import proguard.testutils.match
import proguard.testutils.runClassPool

class TestTypeTransformerMergeIZArray : FreeSpec({

    "Given a smali file that tries to merge integer and boolean types" - {

        "When loading and translating the file" - {

            val (pcp, _) = ClassPoolBuilder.fromSmali(
                SmaliSource(
                    "TestTypeTransformerMergeIZArray.smali",
                    """
                    .class public LTestTypeTransformerMergeIZArray;
                    .super Ljava/lang/Object;
                    
                    .method public static foo()Z
                        .registers 2
                    
                        const/4 v0, 0x2
                        new-array v0, v0, [I
                    
                        fill-array-data v0, :array_e
                        const/4 v1, 0x0
                        aget v0, v0, v1
                    
                        return v0
                    
                        :array_e
                        .array-data 4
                            0x0
                            0x1
                        .end array-data
                    .end method
                    
                    
                    
                    .method public static final main([Ljava/lang/String;)V
                        .registers 4
                    
                        invoke-static {}, LTestTypeTransformerMergeIZArray;->foo()Z
                        move-result v3
                        sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
                        new-instance v2, Ljava/lang/StringBuilder;
                        invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
                        const-string v3, "The answer is "
                        invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                        if-nez v3, :cond_14
                        const/4 v3, 0x0
                        goto :goto_16
                        :cond_14
                        const/16 v3, 0x2a
                        :goto_16
                        invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
                        invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                        move-result-object v3
                        invoke-virtual {v1, v3}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                    
                        return-void
                    .end method
                    """.trimIndent()
                )
            )
            val testClass = pcp.getClass("TestTypeTransformerMergeIZArray")
            val methodFoo = testClass.findMethod("foo", "()Z")
            val methodMain = testClass.findMethod("main", "([Ljava/lang/String;)V")

            "Then the classpool should not be null" {
                pcp shouldNotBe null
            }

            "Then the classpool should contain the TestTypeTransformerMergeIZArray class" {
                testClass shouldNotBe null
            }

            "Then the superclass of TestTypeTransformerMergeIZArray should be Object" {
                testClass.superName shouldBe "java/lang/Object"
            }

            "Then the class TestTypeTransformerMergeIZArray should contain two methods: foo and main" {
                methodFoo shouldNotBe null
                methodMain shouldNotBe null
            }

            "Then the instruction sequence of method `foo` should match that of dex2jar after translation" {
                with(testClass and methodFoo) {
                    match {
                        iconst_2()
                        newarray(10)
                        astore_0()
                        aload_0()
                        dup()
                        ldc(0)
                        iconst_0()
                        iastore()
                        dup()
                        ldc(1)
                        iconst_1()
                        iastore()
                        pop()
                        aload_0()
                        iconst_0()
                        iaload()
                        ireturn()
                    } shouldBe true
                }
            }

            "Then the instruction sequence of method `main` should match that of dex2jar after translation" {
                // FIXME: adding labels to the instruction sequence causes the match to fail
                // LABELS
                val editor = CodeAttributeEditor()
                val L0 = editor.label()
                val L1 = editor.label()

                with(testClass and methodMain) {
                    match {
                        invokestatic("TestTypeTransformerMergeIZArray", "foo", "()Z")
                        pop()
                        getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                        astore_0()
                        new_("java/lang/StringBuilder")
                        dup()
                        invokespecial("java/lang/StringBuilder", "<init>", "()V")
                        astore_1()
                        aload_1()
                        ldc("The answer is ")
                        invokevirtual("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                        pop()
                        ldc("The answer is ")
//                      ifnonnull(L0.offset())
                        ifnonnull(8) // jumps to label L0
                        iconst_0()
                        istore_2()
//                      goto_(L1.offset())
                        goto_(6) // jumps to label L1
//                      label(L0)
                        bipush(42)
                        istore_2()
//                      label(L1)
                        aload_1()
                        iload_2()
                        invokevirtual("java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;")
                        pop()
                        aload_0()
                        aload_1()
                        invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;")
                        invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                        return_()
                    }
                } shouldBe true
            }

            "Then the result after execution of the program should be correct" {
                val expected = "The answer is 42\n"
                val output = runClassPool(pcp, "TestTypeTransformerMergeIZArray", arrayOf())

                output shouldBe expected
            }
        }
    }
})
