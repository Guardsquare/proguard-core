/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
 */

package proguard.classfile.editor

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.classfile.AccessConstants.PUBLIC
import proguard.classfile.Clazz
import proguard.classfile.VersionConstants.CLASS_VERSION_1_6
import proguard.classfile.constant.ClassConstant
import proguard.classfile.constant.Constant
import proguard.classfile.constant.FloatConstant
import proguard.classfile.constant.IntegerConstant
import proguard.classfile.constant.PrimitiveArrayConstant
import proguard.classfile.constant.StringConstant
import proguard.classfile.constant.visitor.ConstantVisitor
import proguard.classfile.instruction.Instruction.OP_LDC
import proguard.classfile.instruction.Instruction.OP_LDC_W

class InstructionSequenceBuilderTest : BehaviorSpec({
    Given("An instructionSequenceBuilder") {
        val builder = InstructionSequenceBuilder()

        When("We insert an int ldc instruction") {
            var value = 0
            builder.ldc(
                42,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitIntegerConstant(clazz: Clazz, integerConstant: IntegerConstant) {
                        value = integerConstant.value
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe 42
            }
        }

        When("We insert a float ldc instruction") {
            var value = 0f
            builder.ldc(
                42.0f,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitFloatConstant(clazz: Clazz, floatConstant: FloatConstant) {
                        value = floatConstant.value
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe 42.0f
            }
        }

        When("We insert a long ldc instruction") {
            var value = 0L
            builder.ldc(
                42L,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitPrimitiveArrayConstant(clazz: Clazz, primitiveArrayConstant: PrimitiveArrayConstant) {
                        value = (primitiveArrayConstant.getValues() as Long?)!!
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe 42L
            }
        }

        When("We insert a double ldc instruction") {
            var value = 0.0
            builder.ldc(
                42.0,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitPrimitiveArrayConstant(clazz: Clazz, primitiveArrayConstant: PrimitiveArrayConstant) {
                        value = (primitiveArrayConstant.getValues() as Double?)!!
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe 42.0
            }
        }

        When("We insert a string ldc instruction") {
            var value = ""
            builder.ldc(
                "Hello, World!",
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant?) {}

                    override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
                        value = stringConstant.getString(clazz)
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe "Hello, World!"
            }
        }

        When("We insert an array ldc instruction") {
            var value = intArrayOf(0)
            builder.ldc(
                intArrayOf(42),
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitPrimitiveArrayConstant(clazz: Clazz, primitiveArrayConstant: PrimitiveArrayConstant) {
                        value = (primitiveArrayConstant.values as IntArray?)!!
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value[0] shouldBe 42
            }
        }

        When("We insert a class ldc instruction") {
            var value = ""
            val clazz = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object",
            )
                .programClass
            builder.ldc(
                clazz,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitClassConstant(clazz: Clazz, classConstant: ClassConstant) {
                        value = classConstant.getName(clazz)
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe "TestClass"
            }
        }

        When("We insert a member ldc instruction") {
            var value = ""
            val classBuilder = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object",
            )
            val method = classBuilder.addAndReturnMethod(
                PUBLIC,
                "testMethod",
                "()V",
            )
            val clazz = classBuilder.programClass
            builder.ldc(
                clazz,
                method,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
                        value = stringConstant.getString(clazz)
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC
            }

            Then("The given visitor should have been called") {
                value shouldBe "testMethod"
            }
        }

        When("We insert an int ldc_w instruction") {
            var value = 0
            builder.ldc_w(
                42,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitIntegerConstant(clazz: Clazz, integerConstant: IntegerConstant) {
                        value = integerConstant.value
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value shouldBe 42
            }
        }

        When("We insert a float ldc_w instruction") {
            builder.ldc_w(42.0f)

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }
        }

        When("We insert a long ldc_w instruction") {
            var value = 0L
            builder.ldc_w(
                42L,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitPrimitiveArrayConstant(clazz: Clazz, primitiveArrayConstant: PrimitiveArrayConstant) {
                        value = (primitiveArrayConstant.getValues() as Long?)!!
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value shouldBe 42L
            }
        }

        When("We insert a double ldc_w instruction") {
            var value = 0.0
            builder.ldc_w(
                42.0,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitPrimitiveArrayConstant(clazz: Clazz, primitiveArrayConstant: PrimitiveArrayConstant) {
                        value = (primitiveArrayConstant.getValues() as Double?)!!
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value shouldBe 42.0
            }
        }

        When("We insert a string ldc_w instruction") {
            var value = ""
            builder.ldc_w(
                "Hello, World!",
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
                        value = stringConstant.getString(clazz)
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value shouldBe "Hello, World!"
            }
        }

        When("We insert an array ldc_w instruction") {
            var value = intArrayOf(0)
            builder.ldc_w(
                intArrayOf(42),
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitPrimitiveArrayConstant(clazz: Clazz, primitiveArrayConstant: PrimitiveArrayConstant) {
                        value = (primitiveArrayConstant.values as IntArray?)!!
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value[0] shouldBe 42
            }
        }

        When("We insert a class ldc_w instruction") {
            var value = ""
            val clazz = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object",
            )
                .programClass
            builder.ldc_w(
                clazz,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitClassConstant(clazz: Clazz, classConstant: ClassConstant) {
                        value = classConstant.getName(clazz)
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value shouldBe "TestClass"
            }
        }

        When("We insert a member ldc_w instruction") {
            var value = ""
            val classBuilder = ClassBuilder(
                CLASS_VERSION_1_6,
                PUBLIC,
                "TestClass",
                "java/lang/Object",
            )
            val method = classBuilder.addAndReturnMethod(
                PUBLIC,
                "testMethod",
                "()V",
            )
            val clazz = classBuilder.programClass
            builder.ldc_w(
                clazz,
                method,
                object : ConstantVisitor {
                    override fun visitAnyConstant(clazz: Clazz, constant: Constant) {}

                    override fun visitStringConstant(clazz: Clazz, stringConstant: StringConstant) {
                        value = stringConstant.getString(clazz)
                    }
                },
            )

            Then("The resulting instructions array will contain the ldc_w instruction") {
                val instructions = builder.instructions()

                instructions.size shouldBe 1
                instructions[0].opcode shouldBe OP_LDC_W
            }

            Then("The given visitor should have been called") {
                value shouldBe "testMethod"
            }
        }
    }
})
