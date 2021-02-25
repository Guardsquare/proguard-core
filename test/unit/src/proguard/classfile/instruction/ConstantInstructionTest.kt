/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.instruction

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.Clazz
import proguard.classfile.Method
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.visitor.AllAttributeVisitor
import proguard.classfile.instruction.Instruction.OP_LDC
import proguard.classfile.instruction.visitor.AllInstructionVisitor
import proguard.classfile.instruction.visitor.InstructionVisitor
import proguard.util.ClassPoolBuilder.fromStrings as ClassPool

class ConstantInstructionTest : FreeSpec({

    "Constant instructions should be" - {
        "throwing if they have a class constant operand" {
            val classPool = ClassPool("""
                public class Foo {
                    public void bar() {
                        System.out.println(Foo.class);
                    }
                }
            """.trimIndent())

            val clazz = classPool.getClass("Foo")
            val method = clazz.findMethod("bar", "()V")
            var throwingLdcCount = 0

            method.accept(clazz,
                AllAttributeVisitor(
                AllInstructionVisitor(
                object : InstructionVisitor {
                    override fun visitAnyInstruction(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?, offset: Int, instruction: Instruction?) { }
                    override fun visitConstantInstruction(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?, offset: Int, constantInstruction: ConstantInstruction?) {
                        if (constantInstruction?.opcode == OP_LDC && constantInstruction.mayInstanceThrowExceptions(clazz)) throwingLdcCount++
                    }
                })))

            throwingLdcCount shouldBe 1
        }

        "not be throwing if they don't have a class constant operand" {
            val classPool = ClassPool("""
                public class Foo {
                    public void bar() {
                        System.out.println("constant");
                    }
                }
            """.trimIndent())

            val clazz = classPool.getClass("Foo")
            val method = clazz.findMethod("bar", "()V")
            var throwingLdcCount = 0

            method.accept(clazz,
                AllAttributeVisitor(
                AllInstructionVisitor(
                object : InstructionVisitor {
                    override fun visitAnyInstruction(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?, offset: Int, instruction: Instruction?) { }
                    override fun visitConstantInstruction(clazz: Clazz?, method: Method?, codeAttribute: CodeAttribute?, offset: Int, constantInstruction: ConstantInstruction?) {
                        if (constantInstruction?.opcode == OP_LDC && constantInstruction.mayInstanceThrowExceptions(clazz)) throwingLdcCount++
                    }
                })))

            throwingLdcCount shouldBe 0
        }
    }
})
