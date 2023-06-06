/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.HashMapAbstractState
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode
import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState
import proguard.analysis.datastructure.callgraph.SymbolicCall
import proguard.classfile.Clazz
import proguard.classfile.MethodDescriptor
import proguard.classfile.MethodSignature
import proguard.classfile.ProgramClass
import proguard.classfile.ProgramMethod
import proguard.classfile.Signature
import proguard.classfile.attribute.CodeAttribute
import proguard.classfile.constant.ClassConstant
import proguard.classfile.constant.Constant
import proguard.classfile.constant.DoubleConstant
import proguard.classfile.constant.FieldrefConstant
import proguard.classfile.constant.FloatConstant
import proguard.classfile.constant.IntegerConstant
import proguard.classfile.constant.LongConstant
import proguard.classfile.constant.MethodrefConstant
import proguard.classfile.constant.NameAndTypeConstant
import proguard.classfile.constant.Utf8Constant
import proguard.classfile.instruction.BranchInstruction
import proguard.classfile.instruction.ConstantInstruction
import proguard.classfile.instruction.Instruction
import proguard.classfile.instruction.InstructionFactory
import proguard.classfile.instruction.LookUpSwitchInstruction
import proguard.classfile.instruction.SimpleInstruction
import proguard.classfile.instruction.TableSwitchInstruction
import proguard.classfile.instruction.VariableInstruction
import proguard.evaluation.value.BasicValueFactory.UNKNOWN_VALUE
import proguard.evaluation.value.ParticularDoubleValue
import proguard.evaluation.value.ParticularFloatValue
import proguard.evaluation.value.ParticularIntegerValue
import proguard.evaluation.value.ParticularLongValue
import proguard.evaluation.value.ParticularReferenceValue
import proguard.testutils.ClassPoolBuilder
import proguard.testutils.JavaSource
import proguard.testutils.cpa.ExpressionAbstractState
import proguard.testutils.cpa.ExpressionTransferRelation
import proguard.testutils.cpa.InstructionExpression
import proguard.testutils.cpa.MethodExpression
import proguard.testutils.cpa.ValueExpression
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

class JvmTransferRelationTest : FreeSpec({

    fun inverseConstantLookup(constantPool: Array<Constant?>, constant: Constant): Int {
        for (i in constantPool.indices) {
            if (constantPool[i] == null || constantPool[i]!!.javaClass != constant.javaClass) {
                continue
            }
            when (constant) {
                is IntegerConstant ->
                    if (constant.value == (constantPool[i] as IntegerConstant).value)
                        return i
                is FloatConstant ->
                    if (constant.value == (constantPool[i] as FloatConstant).value)
                        return i
                is LongConstant ->
                    if (constant.value == (constantPool[i] as LongConstant).value)
                        return i
                is DoubleConstant ->
                    if (constant.value == (constantPool[i] as DoubleConstant).value)
                        return i
                is FieldrefConstant -> {
                    val fieldref = constantPool[i] as FieldrefConstant
                    if (constant.referencedClass == fieldref.referencedClass &&
                        constant.nameAndTypeIndex == fieldref.nameAndTypeIndex
                    )
                        return i
                }
                is MethodrefConstant -> {
                    val methodref = constantPool[i] as MethodrefConstant
                    if (constant.u2classIndex == methodref.u2classIndex &&
                        constant.nameAndTypeIndex == methodref.nameAndTypeIndex
                    )
                        return i
                }
                is ClassConstant ->
                    if (constant.referencedClass == (constantPool[i] as ClassConstant).referencedClass)
                        return i
                is NameAndTypeConstant -> {
                    val nameAndType = constantPool[i] as NameAndTypeConstant
                    if (constant.u2nameIndex == nameAndType.u2nameIndex &&
                        constant.u2descriptorIndex == nameAndType.u2descriptorIndex
                    )
                        return i
                }
                is Utf8Constant ->
                    if (constant.string == (constantPool[i] as Utf8Constant).string)
                        return i
            }
        }
        return -1
    }

    fun inverseFieldConstantLookup(
        constantPool: Array<Constant?>,
        clazz: Clazz,
        name: String,
        type: String
    ): Int {
        return inverseConstantLookup(
            constantPool,
            FieldrefConstant(
                inverseConstantLookup(
                    constantPool,
                    ClassConstant(
                        -1,
                        clazz
                    )
                ),
                inverseConstantLookup(
                    constantPool,
                    NameAndTypeConstant(
                        inverseConstantLookup(
                            constantPool,
                            Utf8Constant(name)
                        ),
                        inverseConstantLookup(
                            constantPool,
                            Utf8Constant(type)
                        )
                    )
                ),
                clazz,
                null
            )
        )
    }

    fun inverseMethodConstantLookup(
        constantPool: Array<Constant?>,
        clazz: Clazz,
        name: String,
        type: String
    ): Int {
        return inverseConstantLookup(
            constantPool,
            MethodrefConstant(
                inverseConstantLookup(
                    constantPool,
                    ClassConstant(
                        -1,
                        clazz
                    )
                ),
                inverseConstantLookup(
                    constantPool,
                    NameAndTypeConstant(
                        inverseConstantLookup(
                            constantPool,
                            Utf8Constant(name)
                        ),
                        inverseConstantLookup(
                            constantPool,
                            Utf8Constant(type)
                        )
                    )
                ),
                clazz,
                null
            )
        )
    }

    val programClassPool = ClassPoolBuilder.fromSource(
        JavaSource(
            "A.java",
            """
                class A {
                    public static int     i;
                    public static double  d;
                    public        float   f;
                    public        long    l;
                
                    public static int foo(int i, double d) {
                        return 0;
                    }
               
                    double fun() {
                        return 0.0;
                    }
                
                    public byte foo() {
                        return 1;
                    }
                    
                    public void apply(I i) {
                        i.test(true);
                    }
                
                    public void main(){
                        double pi = 3.14;
                        int j = 1000000;
                        i = j;
                        d = j;
                        l = j;
                        B.i = i;
                        B.d = i;
                        foo();
                        foo(0, 1.1);
                        f = 5.0F;
                        l = i;
                        new B();
                        fun();
                        apply(x -> foo() == 1); // calls a lambda factory with A.this as an argument
                    }
                }
            
                class B {
                    public static int    i;
                    public static double d;
                }
                
                interface I {
                    boolean test(boolean x);
                }
            """.trimIndent()
        )
    ).programClassPool

    /**
     * [ExpressionTransferRelation] is used for testing. The [ExpressionAbstractState]
     * contains a computational tree transparent to the concrete values, instructions, and methods.
     * Thus, one can trace back how the abstract state is obtained.
     * Since the [IntraproceduralTransferRelation] uses a padded representation of the category 2
     * computational types, most tests are duplicated to check the padding and order preservation.
     */
    val clazzA = programClassPool.getClass("A") as ProgramClass
    val clazzB = programClassPool.getClass("B") as ProgramClass
    val transferRelation = ExpressionTransferRelation()
    val getAbstractSuccessorForInstruction = transferRelation::class.functions
        .find { it.name == "getAbstractSuccessorForInstruction" }!!
    getAbstractSuccessorForInstruction.isAccessible = true
    val processCall = transferRelation::class.functions
        .find { it.name == "processCall" }!!
    processCall.isAccessible = true
    val node = JvmCfaNode(ArrayList(), ArrayList(), null, 0, clazzA)
    val emptyState = JvmAbstractState<ExpressionAbstractState>(
        node,
        JvmFrameAbstractState<ExpressionAbstractState>(),
        JvmForgetfulHeapAbstractState<ExpressionAbstractState>(ExpressionAbstractState(setOf(ValueExpression(UNKNOWN_VALUE)))),
        HashMapAbstractState<String, ExpressionAbstractState>()
    )

    /**
     * Arithmetic instructions should result in a corresponding computational tree.
     */
    "Arithmetic instructions are handled correctly" - {
        "Category 1" {
            val state = emptyState.copy()
            state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
            state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
            val result = emptyState.copy()
            result.push(
                ExpressionAbstractState(
                    setOf(
                        InstructionExpression(
                            SimpleInstruction(Instruction.OP_IADD),
                            listOf(
                                ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                                ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2))))
                            )
                        )
                    )
                )
            )
            getAbstractSuccessorForInstruction.call(
                transferRelation,
                state,
                SimpleInstruction(Instruction.OP_IADD),
                clazzA,
                null
            ) shouldBe result
        }

        "Category 2" {
            val state = emptyState.copy()
            state.push(transferRelation.abstractDefault)
            state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(1)))))
            state.push(transferRelation.abstractDefault)
            state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(2)))))
            val result = emptyState.copy()
            result.push(transferRelation.abstractDefault)
            result.push(
                ExpressionAbstractState(
                    setOf(
                        InstructionExpression(
                            SimpleInstruction(Instruction.OP_DADD),
                            listOf(
                                ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(1)))),
                                transferRelation.abstractDefault,
                                ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(2)))),
                                transferRelation.abstractDefault
                            )
                        )
                    )
                )
            )
            getAbstractSuccessorForInstruction.call(
                transferRelation,
                state,
                SimpleInstruction(Instruction.OP_DADD),
                clazzA,
                null
            ) shouldBe result
        }

        "Long shift" {
            val state = emptyState.copy()
            state.push(transferRelation.abstractDefault)
            state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(2)))))
            state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
            val result = emptyState.copy()
            result.push(transferRelation.abstractDefault)
            result.push(
                ExpressionAbstractState(
                    setOf(
                        InstructionExpression(
                            SimpleInstruction(Instruction.OP_LSHL),
                            listOf(
                                ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(2)))),
                                transferRelation.abstractDefault,
                                ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1))))
                            )
                        )
                    )
                )
            )
            getAbstractSuccessorForInstruction.call(
                transferRelation,
                state,
                SimpleInstruction(Instruction.OP_LSHL),
                clazzA,
                null
            ) shouldBe result
        }
    }

    /**
     * Stack swap/duplication should comply with the JVM behavior.
     */
    "Stack rearrangement works correctly" {
        /**
         *  | 1 |    | 1 |
         *  |   | => | 1 |
         *  |___|    |___|
         */
        var state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        var result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_DUP),
            clazzA,
            null
        ) shouldBe result

        /**
         *  | 1 |    | 1 |
         *  | 2 | => | 2 |
         *  |   |    | 1 |
         *  |___|    |___|
         */
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_DUP_X1),
            clazzA,
            null
        ) shouldBe result

        /**
         *  | 1 |    | 1 |
         *  | 2 |    | 2 |
         *  | 3 | => | 3 |
         *  |   |    | 1 |
         *  |___|    |___|
         */
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_DUP_X2),
            clazzA,
            null
        ) shouldBe result

        /**
         *  | 1 |    | 1 |
         *  | 2 |    | 2 |
         *  |   | => | 1 |
         *  |   |    | 2 |
         *  |___|    |___|
         */
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_DUP2),
            clazzA,
            null
        ) shouldBe result

        /**
         *  | 1 |    | 1 |
         *  | 2 |    | 2 |
         *  | 3 | => | 3 |
         *  |   |    | 1 |
         *  |   |    | 2 |
         *  |___|    |___|
         */
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_DUP2_X1),
            clazzA,
            null
        ) shouldBe result

        /**
         *  | 1 |    | 1 |
         *  | 2 |    | 2 |
         *  | 3 |    | 3 |
         *  | 4 | => | 4 |
         *  |   |    | 1 |
         *  |   |    | 2 |
         *  |___|    |___|
         */
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(4)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(4)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_DUP2_X2),
            clazzA,
            null
        ) shouldBe result

        /**
         *  | 1 |    | 2 |
         *  | 2 | => | 1 |
         *  |___|    |___|
         */
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_SWAP),
            clazzA,
            null
        ) shouldBe result
    }

    "Local variables are processed correctly" - {
        /**
         * Loading local variables should copy the value from the variable array to the top of the operand stack.
         * Here, the indexing is checked.
         */
        "Loading" - {
            /**
             *  [ 1 ]    [ 1 ]
             *
             *  |   | => | 1 |
             *  |___|    |___|
             */
            "Category 1" {
                var state = emptyState.copy()
                state.setVariable(
                    0,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                var result = emptyState.copy()
                result.setVariable(
                    0,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_ILOAD_0),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                state.setVariable(
                    5,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result = emptyState.copy()
                result.setVariable(
                    5,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_ILOAD, 5),
                    clazzA,
                    null
                ) shouldBe result
            }

            /**
             *  [1,2]    [1,2]
             *
             *  |   |    | 1 |
             *  |   | => | 2 |
             *  |___|    |___|
             */
            "Category 2" {
                var state = emptyState.copy()
                state.setVariable(
                    0,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                var result = emptyState.copy()
                result.setVariable(
                    0,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result.push(transferRelation.abstractDefault)
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_DLOAD_0),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                state.setVariable(
                    5,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result = emptyState.copy()
                result.setVariable(
                    5,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result.push(transferRelation.abstractDefault)
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_DLOAD, 5),
                    clazzA,
                    null
                ) shouldBe result
            }
        }

        /**
         * Storing local variables should move the value from the top of the operand stack to the variable array.
         * Here, the indexing is checked.
         */
        "Storing" - {
            /**
             *  [ * ]    [ 1 ]
             *
             *  | 1 | => |   |
             *  |___|    |___|
             */
            "Category 1" {
                var state = emptyState.copy()
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                var result = emptyState.copy()
                result.setVariable(
                    0,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_ISTORE_0),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                result = emptyState.copy()
                result.setVariable(
                    5,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result.setVariable(
                    5,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_ISTORE, 5),
                    clazzA,
                    null
                ) shouldBe result
            }

            /**
             *  [*,*]    [1,2]
             *
             *  | 1 | => |   |
             *  | 2 |    |   |
             *  |___|    |___|
             */
            "Category 2" {
                var state = emptyState.copy()
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
                var result = emptyState.copy()
                result.setVariable(
                    0,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))),
                    transferRelation.abstractDefault
                )
                result.setVariable(
                    1,
                    ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))),
                    transferRelation.abstractDefault
                )
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    VariableInstruction(Instruction.OP_DSTORE_0),
                    clazzA,
                    null
                ) shouldBe result
            }
        }

        "Incrementing" {
            var state = emptyState.copy()
            state.setVariable(
                0,
                ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))),
                transferRelation.abstractDefault
            )
            var result = emptyState.copy()
            result.setVariable(
                0,
                ExpressionAbstractState(
                    setOf(
                        InstructionExpression(
                            VariableInstruction(Instruction.OP_IINC, 0, 1),
                            listOf(
                                ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2))))
                            )
                        )
                    )
                ),
                transferRelation.abstractDefault
            )
            val res = getAbstractSuccessorForInstruction.call(
                transferRelation,
                state,
                VariableInstruction(Instruction.OP_IINC, 0, 1),
                clazzA,
                null
            )
            res shouldBe result
        }
    }

    /**
     * Constants are looked up in the runtime constant pool and pushed into the operand stack.
     */
    "Constants are loaded correctly" - {
        /**
         *  |   | => | 1 |
         *  |___|    |___|
         */
        "Category 1" {
            val state = emptyState.copy()
            val result = state.copy()
            result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1000000)))))
            getAbstractSuccessorForInstruction.call(
                transferRelation,
                state,
                ConstantInstruction(
                    Instruction.OP_LDC,
                    inverseConstantLookup(clazzA.constantPool, IntegerConstant(1000000))
                ),
                clazzA,
                null
            ) shouldBe result
        }

        /**
         *  |   |    | 1 |
         *  |   | => | 2 |
         *  |___|    |___|
         */
        "Category 2" {
            val state = emptyState.copy()
            val result = state.copy()
            result.push(transferRelation.abstractDefault)
            result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(3.14)))))
            getAbstractSuccessorForInstruction.call(
                transferRelation,
                state,
                ConstantInstruction(
                    Instruction.OP_LDC2_W,
                    inverseConstantLookup(clazzA.constantPool, DoubleConstant(3.14))
                ),
                clazzA,
                null
            ) shouldBe result
        }
    }

    "Static fields are processed correctly" - {
        /**
         * Static field class and name should be looked up in the runtime constant pool, and its value
         * should be copied from the static fields mapping of the abstract state to the top of
         * the operand stack.
         */
        "Loading" - {
            /**
             *  x->1     x->1
             *
             *  |   | => | 1 |
             *  |___|    |___|
             */
            "Category 1" {
                var state = emptyState.copy()
                var value = ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5))))
                state.setStatic("A.i:I", value, transferRelation.abstractDefault)
                var result = state.copy()
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_GETSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzA, "i", "I")
                    ),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                value = ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5))))
                state.setStatic("B.i:I", value, transferRelation.abstractDefault)
                result = state.copy()
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_GETSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzB, "i", "I")
                    ),
                    clazzA,
                    null
                ) shouldBe result
            }

            /**
             *  x->1,2   x->1,2
             *
             *  |   |    | 1 |
             *  |   | => | 2 |
             *  |___|    |___|
             */
            "Category 2" {
                var state = emptyState.copy()
                var value = ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0))))
                state.setStatic("A.d:D", value, transferRelation.abstractDefault)
                var result = state.copy()
                result.push(transferRelation.abstractDefault)
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_GETSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzA, "d", "D")
                    ),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                value = ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0))))
                state.setStatic("B.d:D", value, transferRelation.abstractDefault)
                result = state.copy()
                result.push(transferRelation.abstractDefault)
                result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0)))))
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_GETSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzB, "d", "D")
                    ),
                    clazzA,
                    null
                ) shouldBe result
            }
        }

        /**
         * Static field class and name should be looked up in the runtime constant pool, and it value
         * in the static fields mapping of the abstract state should be replaced by the value from the top of
         * the operand stack.
         */
        "Storing" - {
            /**
             *  x->*     x->1
             *
             *  | 1 | => |   |
             *  |___|    |___|
             */
            "Category 1" {
                var state = emptyState.copy()
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
                var result = emptyState.copy()
                var value = ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5))))
                result.setStatic("A.i:I", value, transferRelation.abstractDefault)
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_PUTSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzA, "i", "I")
                    ),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
                result = emptyState.copy()
                value = ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5))))
                result.setStatic("B.i:I", value, transferRelation.abstractDefault)
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_PUTSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzB, "i", "I")
                    ),
                    clazzA,
                    null
                ) shouldBe result
            }

            /**
             *  x->*,*   x->*,*
             *
             *  | 1 |    |   |
             *  | 2 | => |   |
             *  |___|    |___|
             */
            "Category 2" {
                var state = emptyState.copy()
                state.push(transferRelation.abstractDefault)
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0)))))
                var result = emptyState.copy()
                var value = ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0))))
                result.setStatic("A.d:D", value, transferRelation.abstractDefault)
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_PUTSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzA, "d", "D")
                    ),
                    clazzA,
                    null
                ) shouldBe result

                state = emptyState.copy()
                state.push(transferRelation.abstractDefault)
                state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0)))))
                result = emptyState.copy()
                value = ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0))))
                result.setStatic("B.d:D", value, transferRelation.abstractDefault)
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_PUTSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzB, "d", "D")
                    ),
                    clazzA,
                    null
                ) shouldBe result
            }

            /**
             * The default value shouldn't be set in order to
             * reduce the state space.
             */
            "No static field when setting default value" {
                var state = emptyState.copy()
                state.push(transferRelation.abstractDefault)
                getAbstractSuccessorForInstruction.call(
                    transferRelation,
                    state,
                    ConstantInstruction(
                        Instruction.OP_PUTSTATIC,
                        inverseFieldConstantLookup(clazzA.constantPool, clazzA, "i", "I")
                    ),
                    clazzA,
                    null
                ) shouldBe emptyState
            }
        }
    }

    /**
     * Modifying nonstatic fields does not change the abstract state.
     *
     *
     *  x->*     x->*
     *
     *  | 1 | => |   |
     *  |ref|    |   |
     *  |___|    |___|
     */
    "Nonstatic fields are not modeled" {
        var state = emptyState.copy()
        state.push(transferRelation.abstractDefault)
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularFloatValue(5.0F)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            ConstantInstruction(
                Instruction.OP_PUTFIELD,
                inverseFieldConstantLookup(clazzA.constantPool, clazzA, "f", "F")
            ),
            clazzA,
            null
        ) shouldBe emptyState

        state = emptyState.copy()
        state.push(transferRelation.abstractDefault)
        state.push(transferRelation.abstractDefault)
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(10)))))

        val succ = getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            ConstantInstruction(
                Instruction.OP_PUTFIELD,
                inverseFieldConstantLookup(clazzA.constantPool, clazzA, "l", "J")
            ),
            clazzA,
            null
        )
        succ shouldBe emptyState
    }

    /**
     * Method calls should result in a computational tree containing the method signature and its
     * arguments. For non-static fields the instance should come before other arguments.
     */
    "Method calls are handled correctly" {
        val referenceA = ParticularReferenceValue("LA;", clazzA, null, 0, null)
        val referenceB = ParticularReferenceValue("LB;", clazzB, null, 0, null)
        var state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(referenceA))))
        var result = emptyState.copy()
        result.push(
            ExpressionAbstractState(
                setOf(
                    MethodExpression(
                        "LA;foo()B",
                        listOf(ExpressionAbstractState(setOf(ValueExpression(referenceA))))
                    )
                )
            )
        )
        var output = state.copy()
        processCall.call(
            transferRelation,
            output,
            SymbolicCall(
                null,
                MethodSignature(
                    "A",
                    "foo",
                    MethodDescriptor("()B")
                ),
                0,
                InstructionFactory.create(Instruction.OP_INVOKEVIRTUAL),
                false,
                false
            )
        )
        output shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(referenceA))))
        result = emptyState.copy()
        result.push(transferRelation.abstractDefault)
        result.push(
            ExpressionAbstractState(
                setOf(
                    MethodExpression(
                        "LA;fun()D",
                        listOf(ExpressionAbstractState(setOf(ValueExpression(referenceA))))
                    )
                )
            )
        )
        output = state.copy()
        processCall.call(
            transferRelation,
            output,
            SymbolicCall(
                null,
                MethodSignature(
                    "A",
                    "fun",
                    MethodDescriptor("()D")
                ),
                0,
                InstructionFactory.create(Instruction.OP_INVOKEVIRTUAL),
                false,
                false
            )
        )
        output shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
        state.push(transferRelation.abstractDefault)
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0)))))
        result = emptyState.copy()
        result.push(
            ExpressionAbstractState(
                setOf(
                    MethodExpression(
                        "LA;foo(ID)I",
                        listOf(
                            ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))),
                            ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(5.0)))),
                            transferRelation.abstractDefault
                        )
                    )
                )
            )
        )
        output = state.copy()
        processCall.call(
            transferRelation,
            output,
            SymbolicCall(
                null,
                MethodSignature(
                    "A",
                    "foo",
                    MethodDescriptor("(ID)I")
                ),
                0,
                InstructionFactory.create(Instruction.OP_INVOKESTATIC),
                false,
                false
            )
        )
        output shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(referenceB))))
        output = state.copy()
        processCall.call(
            transferRelation,
            output,
            SymbolicCall(
                null,
                MethodSignature(
                    "A",
                    "<init>",
                    MethodDescriptor("()V")
                ),
                0,
                InstructionFactory.create(Instruction.OP_INVOKESTATIC),
                false,
                false
            )
        )
        output shouldBe state
    }

    /**
     * Heap operations neither modify the abstract state nor return something different from the default value.
     */
    "Heap is not modeled" {
        val classAIndex = inverseConstantLookup(clazzA.constantPool, ClassConstant(-1, clazzA))
        var state = emptyState.copy()
        var result = emptyState.copy()
        result.push(transferRelation.abstractDefault)
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            ConstantInstruction(Instruction.OP_NEW, classAIndex),
            clazzA,
            null
        ) shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
        result = emptyState.copy()
        result.push(transferRelation.abstractDefault)
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            ConstantInstruction(Instruction.OP_NEWARRAY, 0, Instruction.ARRAY_T_INT.toInt()),
            clazzA,
            null
        ) shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(5)))))
        result = emptyState.copy()
        result.push(transferRelation.abstractDefault)
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            ConstantInstruction(Instruction.OP_ANEWARRAY, classAIndex),
            clazzA,
            null
        ) shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        result = emptyState.copy()
        result.push(transferRelation.abstractDefault)
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            ConstantInstruction(Instruction.OP_MULTIANEWARRAY, classAIndex, 3),
            clazzA,
            null
        ) shouldBe result
    }

    /**
     * Same for the synchronization instructions.
     */
    "Synchronization is not modeled" {
        val referenceA = ParticularReferenceValue("LA;", clazzA, null, 0, null)
        var state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(referenceA))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_MONITOREXIT, 0),
            clazzA,
            null
        ) shouldBe emptyState

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(referenceA))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_MONITOREXIT, 0),
            clazzA,
            null
        ) shouldBe emptyState
    }

    /**
     * Same for branching.
     */
    "Branching is not modeled" {
        var state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        var result = emptyState.copy()
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            BranchInstruction(Instruction.OP_IFICMPLE, 23),
            clazzA,
            null
        ) shouldBe result

        state = emptyState.copy()
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            BranchInstruction(Instruction.OP_GOTO, 23),
            clazzA,
            null
        ) shouldBe emptyState
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            BranchInstruction(Instruction.OP_GOTO_W, 23),
            clazzA,
            null
        ) shouldBe emptyState

        state = emptyState.copy()
        result = emptyState.copy()
        result.push(transferRelation.abstractDefault)
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            BranchInstruction(Instruction.OP_JSR, 23),
            clazzA,
            null
        ) shouldBe result

        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            BranchInstruction(Instruction.OP_IFNULL, 23),
            clazzA,
            null
        ) shouldBe emptyState
    }

    /**
     * Same for switches.
     */
    "Switching is not modeled" {
        var state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            LookUpSwitchInstruction(Instruction.OP_LOOKUPSWITCH, 5, null, null),
            clazzA,
            null
        ) shouldBe emptyState
        state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            TableSwitchInstruction(Instruction.OP_TABLESWITCH, 5, 10, 20, null),
            clazzA,
            null
        ) shouldBe emptyState
    }

    // athrow empties the stack except the last value
    "Athrow instruction is handled correctly" {
        var state = emptyState.copy()
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(1)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(2)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(3)))))
        state.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(42)))))
        var result = emptyState.copy()
        result.push(ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(42)))))
        getAbstractSuccessorForInstruction.call(
            transferRelation,
            state,
            SimpleInstruction(Instruction.OP_ATHROW),
            clazzA,
            null
        ) shouldBe result
    }

    "Regression test: successor is created for invoke instruction with no correspondent call edge" - {

        "[36] invokevirtual #10 = Methodref(A.foo()B)" {
            val mainMethod = clazzA.findMethod("main", "()V") as ProgramMethod
            val mainSignature = Signature.of(clazzA, mainMethod) as MethodSignature
            val sourceNode = JvmCfaNode(mainSignature, 36, clazzA)
            val targetNode = JvmCfaNode(mainSignature, 39, clazzA)
            val invokeEdge = JvmInstructionCfaEdge(
                sourceNode,
                targetNode,
                mainMethod.attributes.first { a -> a is CodeAttribute } as CodeAttribute,
                36
            )

            val sourceState = JvmAbstractState(
                sourceNode,
                JvmFrameAbstractState(),
                JvmForgetfulHeapAbstractState(
                    ExpressionAbstractState(
                        setOf(
                            ValueExpression(UNKNOWN_VALUE)
                        )
                    )
                ),
                HashMapAbstractState()
            )
            val referenceA = ParticularReferenceValue("LA;", clazzA, null, 0, null)
            sourceState.push(ExpressionAbstractState(setOf(ValueExpression(referenceA))))

            val successors = transferRelation.getAbstractSuccessors(sourceState, null)
            successors.size shouldBe 1
        }

        "[76] invokedynamic #20, 0 = InvokeDynamic(NameAndType(test, ()LI;), #0)" {
            val mainMethod = clazzA.findMethod("main", "()V") as ProgramMethod
            val mainSignature = Signature.of(clazzA, mainMethod) as MethodSignature
            val sourceNode = JvmCfaNode(mainSignature, 76, clazzA)
            val targetNode = JvmCfaNode(mainSignature, 81, clazzA)
            val invokeEdge = JvmInstructionCfaEdge(
                sourceNode,
                targetNode,
                mainMethod.attributes.first { a -> a is CodeAttribute } as CodeAttribute,
                76
            )

            val sourceState = JvmAbstractState(
                sourceNode,
                JvmFrameAbstractState(),
                JvmForgetfulHeapAbstractState(
                    ExpressionAbstractState(
                        setOf(
                            ValueExpression(UNKNOWN_VALUE)
                        )
                    )
                ),
                HashMapAbstractState()
            )
            val referenceA = ParticularReferenceValue("LA;", clazzA, null, 0, null)
            sourceState.push(ExpressionAbstractState(setOf(ValueExpression(referenceA))))

            val successors = transferRelation.getAbstractSuccessors(sourceState, null)
            successors.size shouldBe 1
        }
    }
})
