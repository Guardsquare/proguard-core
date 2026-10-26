package proguard.classfile.editor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.LineNumberTableAttribute
import proguard.classfile.attribute.LineOrigin
import proguard.classfile.attribute.StructuredLineNumberInfo
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.CodeAttributeFinder
import proguard.testutils.JavaSource

class CodeAttributeEditorTest : BehaviorSpec({

    Given("A method with line numbers") {
        val (programClassPool, libraryClassPool) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
            package com.example;
            
            public class Test {
            
              public static void test() {
                print("Test"); print("Test same line");
                print("Test next line");
              }
            
              public static void print(String message) {
                System.out.println(message);
              }            
            }

                """.trimIndent(),
            ),
        )

        val clazz = programClassPool.getClass("com/example/Test") as ProgramClass
        val method = clazz.findMethod("test", "()V") as ProgramMethod
        val code = CodeAttributeFinder.findCodeAttribute(method)!!

        When("Injecting new instructions with line numbers") {
            val editor = CodeAttributeEditor()
            editor.reset(code.u4codeLength)

            val block = StructuredLineNumberInfo.Block(LineOrigin.SimpleOrigin.COPIED)

            editor.insertBeforeOffset(
                5,
                InstructionSequenceBuilder(clazz)
                    .line(editor.line(100, block))
                    .ldc("Injected test")
                    .invokestatic("com/example/Test", "print", "(Ljava/lang/String;)V")
                    .line(editor.line(CodeAttributeEditor.RESTORE_PREVIOUS_LINE_NUMBER, block))
                    .instructions(),
            )

            editor.visitCodeAttribute(clazz, method, code)

            Then("The line should be restored correctly after the injected code") {
                val lineNumberTableAttribute = code.getAttribute(clazz, Attribute.LINE_NUMBER_TABLE) as LineNumberTableAttribute

                lineNumberTableAttribute.lineNumberTable[0].u2lineNumber shouldBe 6
                lineNumberTableAttribute.lineNumberTable[0].u2startPC shouldBe 0
                lineNumberTableAttribute.lineNumberTable[1].u2lineNumber shouldBe 100
                lineNumberTableAttribute.lineNumberTable[1].u2startPC shouldBe 5
                lineNumberTableAttribute.lineNumberTable[2].u2lineNumber shouldBe 6
                lineNumberTableAttribute.lineNumberTable[2].u2startPC shouldBe 10
            }
        }

        When("Removing the original line numbers") {
            AttributesEditor(clazz, method, code, true).deleteAttribute(Attribute.LINE_NUMBER_TABLE)

            And("Injecting new instructions with line numbers") {
                val editor = CodeAttributeEditor()
                editor.reset(code.u4codeLength)

                val block = StructuredLineNumberInfo.Block(LineOrigin.SimpleOrigin.COPIED)

                editor.insertBeforeOffset(
                    5,
                    InstructionSequenceBuilder(clazz).line(editor.line(100, block)).ldc("Injected test")
                        .invokestatic("com/example/Test", "print", "(Ljava/lang/String;)V")
                        .line(editor.line(CodeAttributeEditor.RESTORE_PREVIOUS_LINE_NUMBER, block)).instructions(),
                )

                editor.visitCodeAttribute(clazz, method, code)

                Then("There should be a dummy entry after the injected code") {
                    val lineNumberTableAttribute =
                        code.getAttribute(clazz, Attribute.LINE_NUMBER_TABLE) as LineNumberTableAttribute

                    lineNumberTableAttribute.lineNumberTable[0].u2lineNumber shouldBe 100
                    lineNumberTableAttribute.lineNumberTable[0].u2startPC shouldBe 5
                    lineNumberTableAttribute.lineNumberTable[1].u2lineNumber shouldBe 0
                    lineNumberTableAttribute.lineNumberTable[1].u2startPC shouldBe 10
                }
            }
        }
    }

    Given("A method with two adjacent injected blocks with magic restore lines") {
        val (programClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Test.java",
                """
                package com.example;

                public class Test {

                  public static void test() {
                    print("A");
                    print("B");
                  }

                  public static void print(String message) {
                    System.out.println(message);
                  }
                }

                """.trimIndent(),
            ),
        )

        val clazz = programClassPool.getClass("com/example/Test") as ProgramClass
        val method = clazz.findMethod("test", "()V") as ProgramMethod
        val code = CodeAttributeFinder.findCodeAttribute(method)!!

        When("Editing two adjacent regions, each ending on a magic restore line") {
            val editor = CodeAttributeEditor()
            editor.reset(code.u4codeLength)

            val blockA = StructuredLineNumberInfo.Block(LineOrigin.SimpleOrigin.COPIED)
            val blockB = StructuredLineNumberInfo.Block(LineOrigin.SimpleOrigin.COPIED)

            // First replacement: nothing follows its magic restore line, so the
            // magic line shares its start PC with the next injected line label.
            editor.replaceInstruction(
                0,
                InstructionSequenceBuilder(clazz)
                    .line(editor.line(100, blockA))
                    .iconst_0()
                    .pop()
                    .line(editor.line(CodeAttributeEditor.RESTORE_PREVIOUS_LINE_NUMBER, blockA))
                    .instructions(),
            )

            // Second replacement: starts on its line label and keeps an
            // instruction after its magic restore line.
            editor.replaceInstruction(
                2,
                InstructionSequenceBuilder(clazz)
                    .line(editor.line(200, blockB))
                    .iconst_1()
                    .pop()
                    .line(editor.line(CodeAttributeEditor.RESTORE_PREVIOUS_LINE_NUMBER, blockB))
                    .iconst_2()
                    .pop()
                    .instructions(),
            )

            editor.visitCodeAttribute(clazz, method, code)

            Then("No line number keeps the magic value") {
                val lineNumberTableAttribute =
                    code.getAttribute(clazz, Attribute.LINE_NUMBER_TABLE) as LineNumberTableAttribute

                // The magic value would overflow the unsigned short line number
                // when writing the class.
                for (index in 0 until lineNumberTableAttribute.u2lineNumberTableLength) {
                    val lineNumber = lineNumberTableAttribute.lineNumberTable[index].u2lineNumber
                    lineNumber shouldBeGreaterThanOrEqual 0
                }
            }
        }
    }
})
