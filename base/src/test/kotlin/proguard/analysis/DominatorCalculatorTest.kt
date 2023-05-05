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

package proguard.analysis

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.DominatorCalculator.ENTRY_NODE_OFFSET
import proguard.analysis.DominatorCalculator.EXIT_NODE_OFFSET
import proguard.classfile.Clazz
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.attribute.ExceptionInfo
import proguard.classfile.attribute.visitor.AttributeVisitor
import proguard.classfile.instruction.Instruction
import proguard.classfile.visitor.ClassVisitor
import proguard.classfile.visitor.MemberVisitor
import java.util.BitSet
import kotlin.streams.asSequence

class DominatorCalculatorTest : FreeSpec({

    class NamedClass(val memberName: String) : ProgramClass() {
        private var superNameStr: String? = null
        override fun getName(): String {
            return memberName
        }

        fun setSuperName(superNameStr: String) {
            this.superNameStr = superNameStr
        }

        override fun getSuperName(): String {
            return superNameStr ?: ""
        }
    }

    class NamedMember(val memberName: String, val descriptor: String) : ProgramMethod() {
        override fun getName(clazz: Clazz?): String {
            return memberName
        }

        override fun getDescriptor(clazz: Clazz?): String {
            return descriptor
        }

        override fun accept(programClass: ProgramClass?, memberVisitor: MemberVisitor?) {
        }

        override fun referencedClassesAccept(classVisitor: ClassVisitor?) {
        }

        override fun attributesAccept(
            programClass: ProgramClass?,
            attributeVisitor: AttributeVisitor?
        ) {
        }
    }

    /**
     * In order to thoroughly test the calculator,
     * we need access to the raw dominator map.
     * This is private, so we're retrieving it via
     * reflection.
     */
    fun DominatorCalculator.getDominators(offset: Int): Set<Int> {
        val dominatorMapField = this.javaClass.declaredFields
            .find { it.name == "dominatorMap" }!!
        dominatorMapField.isAccessible = true
        val dominatorMap = dominatorMapField.get(this) as Map<Int, BitSet>

        return dominatorMap[offset]!!.stream().asSequence().map { it - 2 }.toSet()
    }

    "Simple" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, // 0  bipush 42
            Instruction.OP_BIPUSH, 43, // 2  bipush 43
            Instruction.OP_BIPUSH, 44, // 4  bipush 44
            Instruction.OP_RETURN //      6  return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
             ENTRY
               |
               0
               |
               2
               |
               4
               |
               6
               |
              EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(4) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 4)
        calculator.getDominators(6) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 4, 6)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(
            ENTRY_NODE_OFFSET,
            0,
            2,
            4,
            6,
            EXIT_NODE_OFFSET
        )
    }

    "Branch" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, //     0  bipush 42
            Instruction.OP_IFICMPNE, 0, 8, // 2  if_icmpne 10 (+8)
            Instruction.OP_BIPUSH, 43, //     5  bipush 43
            Instruction.OP_GOTO, 0, 5, //     7  goto 12 (+5)
            Instruction.OP_BIPUSH, 44, //     10 bipush 44
            Instruction.OP_BIPUSH, 45, //     12 bipush 45
            Instruction.OP_RETURN //          14 return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
             ENTRY
               |
               0
               |
               2
              / \
             5   10
             |   |
             7   |
              \ /
               12
               |
               14
               |
              EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(5) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5)
        calculator.getDominators(7) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5, 7)
        calculator.getDominators(10) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 10)
        calculator.getDominators(12) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 12)
        calculator.getDominators(14) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 12, 14)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(
            ENTRY_NODE_OFFSET,
            0,
            2,
            12,
            14,
            EXIT_NODE_OFFSET
        )
    }

    "Normal loop" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, //                        0  bipush 42
            Instruction.OP_IFICMPNE, 0, 8, //                    2  if_icmpne 10 (+8)
            Instruction.OP_BIPUSH, 43, //                        5  bipush 43
            Instruction.OP_GOTO, 0xff.toByte(), 251.toByte(), // 7  goto 2 (-5)
            Instruction.OP_BIPUSH, 44, //                        10 bipush 44
            Instruction.OP_RETURN //                             12 return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
             ENTRY
               |
               0
               |
               2 <--|
              / \   |
             |   5  |
             |   |  |
             |   7 -|
              \ /
               10
               |
               12
               |
              EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(5) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5)
        calculator.getDominators(7) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5, 7)
        calculator.getDominators(10) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 10)
        calculator.getDominators(12) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 10, 12)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(
            ENTRY_NODE_OFFSET,
            0,
            2,
            10,
            12,
            EXIT_NODE_OFFSET
        )
    }

    "Loop to entrypoint" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, //                        0  bipush 42
            Instruction.OP_IFICMPNE, 0, 8, //                    2  if_icmpne 10 (+8)
            Instruction.OP_BIPUSH, 43, //                        5  bipush 43
            Instruction.OP_GOTO, 0xff.toByte(), 249.toByte(), // 7  goto 0 (-7)
            Instruction.OP_BIPUSH, 44, //                        10 bipush 44
            Instruction.OP_RETURN //                             12 return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
             ENTRY
               |
               0 <--|
               |    |
               2    |
              / \   |
             |   5  |
             |   |  |
             |   7 -|
              \ /
               10
               |
               12
               |
              EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(5) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5)
        calculator.getDominators(7) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5, 7)
        calculator.getDominators(10) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 10)
        calculator.getDominators(12) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 10, 12)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(
            ENTRY_NODE_OFFSET,
            0,
            2,
            10,
            12,
            EXIT_NODE_OFFSET
        )
    }

    "Multiple return statements" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, //     0  bipush 42
            Instruction.OP_IFICMPNE, 0, 6, // 2  if_icmpne 8 (+6)
            Instruction.OP_BIPUSH, 43, //     5  bipush 43
            Instruction.OP_RETURN, //         7  return
            Instruction.OP_RETURN //          8  return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
             ENTRY
               |
               0
               |
               2
              / \
             5   8
             |   |
             7   |
              \ /
              EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(5) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5)
        calculator.getDominators(7) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 5, 7)
        calculator.getDominators(8) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 8)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(
            ENTRY_NODE_OFFSET,
            0,
            2,
            EXIT_NODE_OFFSET
        )
    }

    "Switch statement" {
        val code = byteArrayOf(
            Instruction.OP_LOOKUPSWITCH, 0, 0, 0, // 0  lookupswitch:
            0, 0, 0, 41, //                               default (+41)
            0, 0, 0, 3, //                                3 cases
            0, 0, 0, 1, //                                case 1
            0, 0, 0, 36, //                                 (+36)
            0, 0, 0, 42, //                               case 42
            0, 0, 0, 36, //                                 (+36)
            0, 0, 0, 100, //                              case 100
            0, 0, 0, 39, //                                 (+39)
            Instruction.OP_GOTO, 0, 7, //            36 goto 43 (+7)
            Instruction.OP_BIPUSH, 42, //            39 bipush 42
            Instruction.OP_BIPUSH, 43, //            41 bipush 43
            Instruction.OP_RETURN //                 43 return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
                ENTRY
                  |
                  0
                / | \
               /  | 39
              |   |/
              36 41
               \ /
               43
               |
              EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(36) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 36)
        calculator.getDominators(39) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 39)
        calculator.getDominators(41) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 41)
        calculator.getDominators(43) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 43)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(
            ENTRY_NODE_OFFSET,
            0,
            43,
            EXIT_NODE_OFFSET
        )
    }

    "Try block at start of code" {
        val code = byteArrayOf(
            // try start
            Instruction.OP_BIPUSH, 42, // 0  bipush 42
            Instruction.OP_BIPUSH, 43, // 2  bipush 43
            // try end
            Instruction.OP_GOTO, 0, 7, // 4  goto 11 (+7)
            // catch
            Instruction.OP_BIPUSH, 44, // 7  bipush 44
            Instruction.OP_BIPUSH, 45, // 9  bipush 45
            Instruction.OP_BIPUSH, 46, // 11 bipush 46
            Instruction.OP_RETURN //      13 return
        )
        val exceptions = arrayOf(ExceptionInfo(0, 4, 7, 1))
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(
            0, 1, 0, code.size, code,
            exceptions.size, exceptions, 0, arrayOf()
        )

        val calculator = DominatorCalculator(false)
        codeAttribute.accept(clazz, method, calculator)

        /*
            ENTRY
             / \
            0   7
            |   |
            2   9
            |   |
            4   |
             \ /
              11
              |
              13
              |
             EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(4) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 4)
        calculator.getDominators(7) shouldBe setOf(ENTRY_NODE_OFFSET, 7)
        calculator.getDominators(9) shouldBe setOf(ENTRY_NODE_OFFSET, 7, 9)
        calculator.getDominators(11) shouldBe setOf(ENTRY_NODE_OFFSET, 11)
        calculator.getDominators(13) shouldBe setOf(ENTRY_NODE_OFFSET, 11, 13)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET, 11, 13, EXIT_NODE_OFFSET)
    }

    "Try block partway through code" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, // 0  bipush 42
            // try start
            Instruction.OP_BIPUSH, 43, // 2  bipush 43
            // try end
            Instruction.OP_GOTO, 0, 7, // 4  goto 11 (+7)
            // catch
            Instruction.OP_BIPUSH, 44, // 7  bipush 44
            Instruction.OP_BIPUSH, 45, // 9  bipush 45
            Instruction.OP_BIPUSH, 46, // 11 bipush 46
            Instruction.OP_RETURN //      13 return
        )
        val exceptions = arrayOf(ExceptionInfo(2, 4, 7, 1))
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(
            0, 1, 0, code.size, code,
            exceptions.size, exceptions, 0, arrayOf()
        )

        val calculator = DominatorCalculator(false)
        codeAttribute.accept(clazz, method, calculator)

        /*
            ENTRY
              |
              0
             / \
            2   7
            |   |
            4   9
             \ /
              11
              |
              13
              |
             EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(4) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 4)
        calculator.getDominators(7) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 7)
        calculator.getDominators(9) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 7, 9)
        calculator.getDominators(11) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 11)
        calculator.getDominators(13) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 11, 13)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 11, 13, EXIT_NODE_OFFSET)
    }

    "Different try blocks pointing to the same handler" {
        // In Java, nested try blocks can compile to multiple exception handlers with a goto between them.
        val code = byteArrayOf(
            // try start
            Instruction.OP_BIPUSH, 42, //   0  bipush 42
            // try end
            Instruction.OP_BIPUSH, 43, //   2  bipush 43
            // try start
            Instruction.OP_BIPUSH, 44, //   4  bipush 44
            // try end
            Instruction.OP_BIPUSH, 45, //   6  bipush 45
            Instruction.OP_RETURN, //       8  return
            // catch
            Instruction.OP_BIPUSH, 45, //   9  bipush 45
            Instruction.OP_GOTO, -1, -3, // 11  goto 8 (-3)
        )
        val exceptions = arrayOf(
            ExceptionInfo(0, 2, 9, 1),
            ExceptionInfo(4, 6, 9, 1),
        )

        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(
            0, 1, 0, code.size, code,
            exceptions.size, exceptions, 0, arrayOf()
        )

        val calculator = DominatorCalculator(false)
        codeAttribute.accept(clazz, method, calculator)

        /*
            ENTRY
             / \
            |   0
            |  /|
            | / 2
            |/  |
            |   4
            |  /|
            | / 6
            |/  |
            9   |
            |   |
            11  |
             \ /
              8
              |
             EXIT
         */

        calculator.getDominators(ENTRY_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET)
        calculator.getDominators(0) shouldBe setOf(ENTRY_NODE_OFFSET, 0)
        calculator.getDominators(2) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2)
        calculator.getDominators(4) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 4)
        calculator.getDominators(6) shouldBe setOf(ENTRY_NODE_OFFSET, 0, 2, 4, 6)
        calculator.getDominators(9) shouldBe setOf(ENTRY_NODE_OFFSET, 9)
        calculator.getDominators(11) shouldBe setOf(ENTRY_NODE_OFFSET, 9, 11)
        calculator.getDominators(8) shouldBe setOf(ENTRY_NODE_OFFSET, 8)
        calculator.getDominators(EXIT_NODE_OFFSET) shouldBe setOf(ENTRY_NODE_OFFSET, 8, EXIT_NODE_OFFSET)
    }

    "Unknown offsets should not have dominators" {
        val code = byteArrayOf(
            Instruction.OP_BIPUSH, 42, // 0  bipush 42
            Instruction.OP_BIPUSH, 43, // 2  bipush 43
            Instruction.OP_BIPUSH, 44, // 4  bipush 44
            Instruction.OP_RETURN //      6  return
        )
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()
        codeAttribute.accept(clazz, method, calculator)

        /*
             ENTRY
               |
               0
               |
               2
               |
               4
               |
               6
               |
              EXIT
         */

        shouldThrow<IllegalStateException> {
            calculator.dominates(0, 1)
        }
        shouldThrow<IllegalStateException> {
            calculator.dominates(0, 3)
        }
        shouldThrow<IllegalStateException> {
            calculator.dominates(0, 5)
        }
        shouldThrow<IllegalStateException> {
            calculator.dominates(0, 7)
        }
    }

    "Empty CodeAttribute" - {
        val code = byteArrayOf()
        val clazz = NamedClass("Test")
        val method = NamedMember("", "()V")
        val codeAttribute = CodeAttribute(0, 1, 0, code.size, code)

        val calculator = DominatorCalculator()

        "Analysis does not crash" {
            shouldNotThrowAny {
                codeAttribute.accept(clazz, method, calculator)
            }
        }

        "All offsets are unknown" {
            shouldThrow<IllegalStateException> {
                calculator.dominates(0, 1)
            }
        }
    }
})
