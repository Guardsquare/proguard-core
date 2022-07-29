package proguard.dexfile

import SmaliSource
import fromSmali
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.classfile.util.InstructionSequenceMatcher
import testutils.ClassPoolBuilder
import testutils.InstructionBuilder

class BasicTranslationTest : FreeSpec({

    "Basic Hello World translation test" - {

        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSmali(
            SmaliSource("HelloWorld.smali", """
                .class public LHelloWorld;
                
                .super Ljava/lang/Object;
                
                .method public static main([Ljava/lang/String;)V
                    .registers 2
                    
                    sget-object     v0, Ljava/lang/System;->out:Ljava/io/PrintStream;
                    
                    const-string	v1, "Hello World!"
                    
                    invoke-virtual  {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
                    
                    return-void
                .end method
            """.trimIndent())
        )

//        programClassPool.classesAccept(ClassPrinter())

        val helloWorldClass = programClassPool.getClass("HelloWorld")

        "Check if classPool contains the HelloWorld class" - {
            helloWorldClass
                .shouldNotBe(null)
        }

        "Check if HelloWorld contains main method" - {
            helloWorldClass
                .findMethod("main", "([Ljava/lang/String;)V")
                .shouldNotBe(null)
        }

        "Check if sequence of operations after translation match original smali code" - {
            val instructionBuilder = InstructionBuilder()

            instructionBuilder
                .getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
                .ldc("Hello World!")
                .invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
                .return_()

            val matcher = InstructionSequenceMatcher(instructionBuilder.constants(), instructionBuilder.instructions())

            // Find the match in the code and print it out.
            class MatchPrinter : InstructionVisitor {
                override fun visitAnyInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, instruction: Instruction) {
                    println(instruction.toString(clazz, offset))
                    instruction.accept(clazz, method, codeAttribute, offset, matcher)
                    if (matcher.isMatching()) {
                        println("  -> matching sequence starting at [" + matcher.matchedInstructionOffset(0) + "]")
                    }
                }
            }

            helloWorldClass.methodsAccept(
                AllAttributeVisitor(
                AllInstructionVisitor(
                MatchPrinter())))
        }
    }

})