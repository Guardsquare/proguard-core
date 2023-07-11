package proguard.classfile.editor

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FreeSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import proguard.classfile.Clazz
import proguard.classfile.Member
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.Attribute
import proguard.classfile.attribute.LineNumberTableAttribute
import proguard.classfile.visitor.MemberVisitor
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.CodeAttributeFinder
import proguard.testutils.ExceptionsAttributeFinder
import proguard.testutils.JavaSource
import proguard.testutils.MethodInstructionCollector

class MethodCopierTest : FreeSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val (programClassPool, _) = ClassPoolBuilder.fromSource(
        JavaSource(
            "Foo.java",
            """
            import java.io.IOException;
            import java.text.ParseException;

            public class Foo {
                public void bar() throws IOException, ParseException {
                    try {
                        System.out.println("Foo");
                    } catch (Exception e) {
                        System.err.println("Failed to print");
                    }
                }
            }
            """.trimIndent()
        ),
        javacArguments = listOf("-source", "1.8", "-target", "1.8")
    )

    val sourceClass = programClassPool.getClass("Foo") as ProgramClass
    val sourceMethod = sourceClass.findMethod("bar", "()V") as ProgramMethod
    val sourceMethodCodeAttribute = CodeAttributeFinder.findCodeAttribute(sourceMethod)!!
    val sourceMethodLineNumberTableAttribute = sourceMethodCodeAttribute.getAttribute(sourceClass, Attribute.LINE_NUMBER_TABLE) as LineNumberTableAttribute

    var copiedMethod: ProgramMethod? = null
    val copiedMethodAssigner = object : MemberVisitor {
        var called = false
        override fun visitAnyMember(clazz: Clazz, member: Member) {}
        override fun visitProgramMethod(programClass: ProgramClass, programMethod: ProgramMethod) {
            copiedMethod = programMethod
            called = true
        }
    }

    "When copying a method into a class that does not already have a matching method" - {
        val (otherProgramClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Baz.java",
                """
                public class Baz {
                    public void biz() {
                        System.out.println("Baz");
                    }
                }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8")
        )

        val bazClass = otherProgramClassPool.getClass("Baz") as ProgramClass
        programClassPool.addClass(bazClass)

        bazClass.accept(
            MethodCopier(
                sourceClass,
                sourceMethod,
                null,
                copiedMethodAssigner
            )
        )

        "Then calling findMethod on the target class should return the copied method" {
            bazClass.findMethod("bar", "()V").shouldNotBeNull() shouldBe copiedMethod.shouldNotBeNull()
        }

        "Then the copied method should have identical access flags to the source method" {
            copiedMethod!!.accessFlags shouldBeExactly sourceMethod.accessFlags
        }

        "Then the copied method should have identical processing flags to the source method" {
            copiedMethod!!.processingFlags shouldBeExactly sourceMethod.processingFlags
        }

        "Then the copied method should have an identical exception table attribute to the source method" {
            val sourceMethodExceptionsAttribute = ExceptionsAttributeFinder.findExceptionTableAttribute(sourceMethod)!!
            val copiedMethodExceptionsAttribute = ExceptionsAttributeFinder.findExceptionTableAttribute(copiedMethod!!)!!

            copiedMethodExceptionsAttribute.u2exceptionIndexTableLength shouldBe sourceMethodExceptionsAttribute.u2exceptionIndexTableLength

            val sourceAttributeName = sourceClass.getString(sourceMethodExceptionsAttribute.u2attributeNameIndex)
            val copiedAttributeName = bazClass.getString(copiedMethodExceptionsAttribute.u2attributeNameIndex)
            copiedAttributeName shouldBe sourceAttributeName

            val sourceExceptionIndexTable = sourceMethodExceptionsAttribute.u2exceptionIndexTable.map { sourceClass.getClassName(it) }
            val copiedExceptionIndexTable = copiedMethodExceptionsAttribute.u2exceptionIndexTable.map { bazClass.getClassName(it) }

            copiedExceptionIndexTable.size shouldBe sourceExceptionIndexTable.size
            copiedExceptionIndexTable shouldContainInOrder sourceExceptionIndexTable
        }

        "For the copied method's code attribute" - {
            val copiedMethodCodeAttribute = CodeAttributeFinder.findCodeAttribute(copiedMethod!!)!!

            "Then the code attribute should exist" {
                copiedMethodCodeAttribute.shouldNotBeNull()
            }

            "Then its code attribute should have functionally identical code to the source method's code attribute" {
                copiedMethodCodeAttribute.u4codeLength shouldBeExactly sourceMethodCodeAttribute.u4codeLength

                val sourceMethodInstructions = MethodInstructionCollector.getMethodInstructions(sourceClass, sourceMethod)
                val copiedMethodInstructions = MethodInstructionCollector.getMethodInstructions(bazClass, copiedMethod)

                copiedMethodInstructions.size shouldBeExactly sourceMethodInstructions.size

                copiedMethodInstructions.forEachIndexed { index, instruction ->
                    val sourceInstruction = sourceMethodInstructions[index]
                    instruction.name shouldBe sourceInstruction.name
                    instruction.opcode shouldBe sourceInstruction.opcode
                }
            }

            "Then its line numbers should refer to the source method" {
                val copiedMethodLineNumberTableAttribute = copiedMethodCodeAttribute.getAttribute(bazClass, Attribute.LINE_NUMBER_TABLE) as LineNumberTableAttribute
                copiedMethodLineNumberTableAttribute.u2lineNumberTableLength shouldBeGreaterThan 0

                val sourceMethodLowestLineNumber = sourceMethodLineNumberTableAttribute.lowestLineNumber
                val sourceMethodHighestLineNumber = sourceMethodLineNumberTableAttribute.highestLineNumber
                val expectedSource = sourceClass.name + "." +
                    sourceMethod.getName(sourceClass) +
                    sourceMethod.getDescriptor(sourceClass) + ":" +
                    sourceMethodLowestLineNumber + ":" +
                    sourceMethodHighestLineNumber

                copiedMethodLineNumberTableAttribute.lineNumberTable.shouldForAll { it.source shouldBe expectedSource }
            }
        }
    }

    "When trying to copy a method into a class which already has a method with the same name and descriptor" - {
        val (otherProgramClassPool, _) = ClassPoolBuilder.fromSource(
            JavaSource(
                "Baz.java",
                """
                public class Baz {
                    public void bar() {
                        System.out.println("Other bar");
                    }
                }
                """.trimIndent()
            ),
            javacArguments = listOf("-source", "1.8", "-target", "1.8")
        )

        val bazClass = otherProgramClassPool.getClass("Baz") as ProgramClass
        programClassPool.addClass(bazClass)

        val existingMethod = bazClass.findMethod("bar", "()V")

        "When not applying a name transformer function" - {
            bazClass.accept(
                MethodCopier(
                    sourceClass,
                    sourceMethod,
                    null,
                    copiedMethodAssigner
                )
            )

            "Then copiedMethodAssigner must not have been called" {
                copiedMethodAssigner.called shouldBe false
            }

            "Then copiedMethod must be null" {
                copiedMethod.shouldBeNull()
            }

            "Then calling findMethod on the target class should return the method that already existed" {
                bazClass.findMethod("bar", "()V").shouldNotBeNull() shouldBe existingMethod.shouldNotBeNull()
            }
        }

        "When applying a name transformer function" - {
            bazClass.accept(
                MethodCopier(
                    sourceClass,
                    sourceMethod,
                    { it + "Copy" },
                    copiedMethodAssigner
                )
            )

            "Then calling findMethod on the target class should return the copied method" {
                bazClass.findMethod("barCopy", "()V").shouldNotBeNull() shouldBe copiedMethod.shouldNotBeNull()
            }
        }
    }
})
