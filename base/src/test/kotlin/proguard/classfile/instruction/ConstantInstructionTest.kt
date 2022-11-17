/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource

class ConstantInstructionTest : FreeSpec({

    "Constant instructions should be" - {
        "throwing if they have a class constant operand" {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "Foo.java",
                    """
                public class Foo {
                    public void bar() {
                        System.out.println(Foo.class);
                    }
                }
                    """.trimIndent()
                ),
                javacArguments = listOf("-source", "1.8", "-target", "1.8")
            )

            val clazz = programClassPool.getClass("Foo")
            val method = clazz.findMethod("bar", "()V")
            var throwingLdcCount = 0

            method.accept(
                clazz,
                AllAttributeVisitor(
                    AllInstructionVisitor(
                        object : InstructionVisitor {
                            override fun visitAnyInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, instruction: Instruction) {}
                            override fun visitConstantInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, constantInstruction: ConstantInstruction) {
                                if (constantInstruction.opcode == OP_LDC && constantInstruction.mayInstanceThrowExceptions(clazz)) throwingLdcCount++
                            }
                        })
                )
            )

            throwingLdcCount shouldBe 1
        }

        "not be throwing if they don't have a class constant operand" {
            val (programClassPool, _) = ClassPoolBuilder.fromSource(
                JavaSource(
                    "Foo.java",
                    """
                public class Foo {
                    public void bar() {
                        System.out.println("constant");
                    }
                }
                    """.trimIndent()
                )
            )

            val clazz = programClassPool.getClass("Foo")
            val method = clazz.findMethod("bar", "()V")
            var throwingLdcCount = 0

            method.accept(
                clazz,
                AllAttributeVisitor(
                    AllInstructionVisitor(
                        object : InstructionVisitor {
                            override fun visitAnyInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, instruction: Instruction) {}
                            override fun visitConstantInstruction(clazz: Clazz, method: Method, codeAttribute: CodeAttribute, offset: Int, constantInstruction: ConstantInstruction) {
                                if (constantInstruction.opcode == OP_LDC && constantInstruction.mayInstanceThrowExceptions(clazz)) throwingLdcCount++
                            }
                        })
                )
            )

            throwingLdcCount shouldBe 0
        }
    }
})
