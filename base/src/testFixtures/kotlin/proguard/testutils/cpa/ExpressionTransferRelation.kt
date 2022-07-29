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

package proguard.testutils.cpa

import proguard.analysis.cpa.jvm.state.JvmAbstractState
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation
import proguard.analysis.datastructure.callgraph.Call
import proguard.classfile.instruction.Instruction
import proguard.classfile.util.ClassUtil
import proguard.evaluation.value.ParticularDoubleValue
import proguard.evaluation.value.ParticularFloatValue
import proguard.evaluation.value.ParticularIntegerValue
import proguard.evaluation.value.ParticularLongValue
import proguard.evaluation.value.ParticularReferenceValue

class ExpressionTransferRelation : JvmTransferRelation<ExpressionAbstractState>() {

    override fun applyInstruction(
        instruction: Instruction?,
        operands: MutableList<ExpressionAbstractState>?,
        resultCount: Int
    ): MutableList<ExpressionAbstractState> {
        return if (resultCount == 2)
            mutableListOf(
                abstractDefault,
                ExpressionAbstractState(
                    setOf(
                        InstructionExpression(
                            instruction!!,
                            operands!!
                        )
                    )
                )
            )
        else
            mutableListOf(
                ExpressionAbstractState(
                    setOf(
                        InstructionExpression(
                            instruction!!,
                            operands!!
                        )
                    )
                )
            )
    }

    override fun getAbstractDoubleConstant(d: Double): MutableList<ExpressionAbstractState> {
        return mutableListOf(
            abstractDefault,
            ExpressionAbstractState(setOf(ValueExpression(ParticularDoubleValue(d))))
        )
    }

    override fun getAbstractFloatConstant(f: Float): ExpressionAbstractState {
        return ExpressionAbstractState(setOf(ValueExpression(ParticularFloatValue(f))))
    }

    override fun getAbstractIntegerConstant(i: Int): ExpressionAbstractState {
        return ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(i))))
    }

    override fun getAbstractLongConstant(l: Long): MutableList<ExpressionAbstractState> {
        return mutableListOf(
            abstractDefault,
            ExpressionAbstractState(setOf(ValueExpression(ParticularLongValue(l))))
        )
    }

    override fun getAbstractNull(): ExpressionAbstractState {
        return ExpressionAbstractState(
            setOf(
                ValueExpression(
                    ParticularReferenceValue(
                        "",
                        null,
                        null,
                        0,
                        null
                    )
                )
            )
        )
    }

    override fun getAbstractShortConstant(s: Short): ExpressionAbstractState {
        return ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(s.toInt()))))
    }

    override fun invokeMethod(
        state: JvmAbstractState<ExpressionAbstractState>?,
        call: Call?,
        operands: MutableList<ExpressionAbstractState>?
    ) {
        when (ClassUtil.internalTypeSize(call!!.target.descriptor.returnType ?: "?")) {
            0 -> return
            1 -> state!!.pushAll(
                mutableListOf(
                    ExpressionAbstractState(
                        setOf(
                            MethodExpression(
                                call.target.fqn ?: "?",
                                operands ?: listOf()
                            )
                        )
                    )
                )
            )
            else -> state!!.pushAll(
                mutableListOf(
                    abstractDefault,
                    ExpressionAbstractState(
                        setOf(
                            MethodExpression(
                                call.target.fqn ?: "?",
                                operands ?: listOf()
                            )
                        )
                    )
                )
            )
        }
    }

    override fun getAbstractDefault(): ExpressionAbstractState {
        return ExpressionAbstractState(setOf(ValueExpression(UnknownValue)))
    }

    override fun getAbstractByteConstant(b: Byte): ExpressionAbstractState {
        return ExpressionAbstractState(setOf(ValueExpression(ParticularIntegerValue(b.toInt()))))
    }
}
