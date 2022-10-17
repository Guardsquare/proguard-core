/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.classfile.editor

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.VersionConstants.CLASS_VERSION_1_6
import proguard.classfile.instruction.Instruction.OP_LDC
import proguard.classfile.instruction.Instruction.OP_LDC_W

class InstructionSequenceBuilderTest : FreeSpec({
    "Given an instructionSequenceBuilder" - {
        val builder = InstructionSequenceBuilder()

        "When we insert an int ldc instruction" - {
            builder.ldc(42)

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert a float ldc instruction" - {
            builder.ldc(42.0f)

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert a long ldc instruction" - {
            builder.ldc(42L)

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert a double ldc instruction" - {
            builder.ldc(42.0)

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert a string ldc instruction" - {
            builder.ldc("Hello, World!")

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert an array ldc instruction" - {
            builder.ldc(intArrayOf(42))

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert a class ldc instruction" - {
            val clazz = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object"
            )
                .programClass
            builder.ldc(clazz)

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert a member ldc instruction" - {
            val classBuilder = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object"
            )
            val method = classBuilder.addAndReturnMethod(
                PUBLIC,
                "testMethod",
                "()V"
            )
            val clazz = classBuilder.programClass
            builder.ldc(clazz, method)

            "Then the resulting instructions array will contain the ldc instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }
        }

        "When we insert an int ldc_w instruction" - {
            builder.ldc_w(42)

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert a float ldc_w instruction" - {
            builder.ldc_w(42.0f)

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert a long ldc_w instruction" - {
            builder.ldc_w(42L)

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert a double ldc_w instruction" - {
            builder.ldc_w(42.0)

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert a string ldc_w instruction" - {
            builder.ldc_w("Hello, World!")

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert an array ldc_w instruction" - {
            builder.ldc_w(intArrayOf(42))

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert a class ldc_w instruction" - {
            val clazz = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object"
            )
                .programClass
            builder.ldc_w(clazz)

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        "When we insert a member ldc_w instruction" - {
            val classBuilder = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object"
            )
            val method = classBuilder.addAndReturnMethod(
                PUBLIC,
                "testMethod",
                "()V"
            )
            val clazz = classBuilder.programClass
            builder.ldc_w(clazz, method)

            "Then the resulting instructions array will contain the ldc_w instruction" {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }
    }
})
