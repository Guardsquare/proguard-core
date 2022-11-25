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

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;
import proguard.analysis.cpa.jvm.util.HeapUtil;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.ConstantInstruction;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.SimpleInstruction;
import proguard.classfile.util.ClassUtil;

/**
 * The {@link JvmTaintTransferRelation} is parametrized by a set of {@link TaintSource} methods.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintTransferRelation
    extends JvmTransferRelation<TaintAbstractState>
{
    private final Map<String, TaintSource> taintSources;

    /**
     * Create a taint transfer relation.
     *
     * @param taintSources a mapping from fully qualified names to taint sources
     */
    public JvmTaintTransferRelation(Map<String, TaintSource> taintSources)
    {
        this.taintSources = taintSources;
    }

    // implementations for JvmTransferRelation

    @Override
    public void invokeMethod(JvmAbstractState<TaintAbstractState> state, Call call, List<TaintAbstractState> operands)
    {
        MethodSignature target = call.getTarget();
        TaintSource detectedSource = taintSources.get(target.getFqn());
        int pushCount = ClassUtil.internalTypeSize(target.descriptor.returnType == null ? "?" : target.descriptor.returnType);
        TaintAbstractState answerContent = operands.stream().reduce(getAbstractDefault(), TaintAbstractState::join);

        // taint the return
        if (detectedSource != null && detectedSource.taintsReturn && !answerContent.contains(detectedSource))
        {
            answerContent = answerContent.copy();
            answerContent.add(detectedSource);
        }

        // pad to the return type size and put the abstract state on the top of the stack
        for (int i = 1; i < pushCount; i++)
        {
            state.push(getAbstractDefault());
        }
        if (pushCount > 0)
        {
            state.push(answerContent);
        }

        // taint static fields
        if (detectedSource == null)
        {
            return;
        }
        Map<String, TaintAbstractState> fqnToValue = new HashMap<>();
        TaintAbstractState newValue = new TaintAbstractState(detectedSource);
        detectedSource.taintsGlobals.forEach(fqn -> fqnToValue.merge(fqn, newValue, TaintAbstractState::join));
        fqnToValue.forEach(state::setStatic);

        // taint heap
        if (!(state.getHeap() instanceof JvmTaintTreeHeapFollowerAbstractState))
        {
            return;
        }
        JvmTaintAbstractState taintAbstractState = (JvmTaintAbstractState) state;
        JvmTaintTreeHeapFollowerAbstractState treeHeap = (JvmTaintTreeHeapFollowerAbstractState) taintAbstractState.getHeap();
        TaintAbstractState detectedTaint = new TaintAbstractState(detectedSource);
        // taint static fields
        detectedSource.taintsGlobals.stream()
                                    .map(JvmStaticFieldLocation::new)
                                    .map(treeHeap::getReferenceAbstractState)
                                    .forEach(r -> taintAbstractState.setObjectTaint(r, detectedTaint));
        // taint arguments
        String descriptor = call.getTarget().descriptor.toString();
        int parameterSize = call.getJvmArgumentSize();
        detectedSource.taintsArgs.stream()
                                 .map(a -> HeapUtil.getArgumentReference(treeHeap, parameterSize, descriptor, call.isStatic(), a - 1))
                                 .forEach(r -> taintAbstractState.setObjectTaint(r, detectedTaint));
        // taint the calling instance
        if (detectedSource.taintsThis)
        {
            taintAbstractState.setObjectTaint(treeHeap.getReferenceAbstractState(new JvmStackLocation(parameterSize - 1)),
                                              detectedTaint);
        }
    }

    @Override
    public TaintAbstractState getAbstractDefault()
    {
        return TaintAbstractState.bottom;
    }

    @Override
    protected JvmAbstractState<TaintAbstractState> getAbstractSuccessorForInstruction(JvmAbstractState<TaintAbstractState> abstractState, Instruction instruction, Clazz clazz, Precision precision)
    {
        instruction.accept(clazz, null, null, 0, new InstructionAbstractInterpreter(abstractState));
        return abstractState;
    }

    protected class InstructionAbstractInterpreter extends JvmTransferRelation<TaintAbstractState>.InstructionAbstractInterpreter
    {

        public InstructionAbstractInterpreter(JvmAbstractState<TaintAbstractState> abstractState)
        {
            super(abstractState);
        }

        @Override
        public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
        {
            switch (simpleInstruction.opcode)
            {
                case Instruction.OP_IALOAD:
                case Instruction.OP_FALOAD:
                case Instruction.OP_AALOAD:
                case Instruction.OP_BALOAD:
                case Instruction.OP_CALOAD:
                case Instruction.OP_SALOAD:
                {
                    TaintAbstractState index = abstractState.pop();
                    abstractState.push(abstractState.getArrayElementOrDefault(new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, abstractState.pop()));
                    break;
                }
                case Instruction.OP_LALOAD:
                case Instruction.OP_DALOAD:
                {
                    TaintAbstractState index = abstractState.pop();
                    abstractState.push(getAbstractDefault());
                    abstractState.push(abstractState.getArrayElementOrDefault(new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, abstractState.pop()));
                    break;
                }
                case Instruction.OP_IASTORE:
                case Instruction.OP_FASTORE:
                case Instruction.OP_AASTORE:
                case Instruction.OP_BASTORE:
                case Instruction.OP_CASTORE:
                case Instruction.OP_SASTORE:
                {
                    TaintAbstractState value = abstractState.pop();
                    TaintAbstractState index = abstractState.pop();
                    abstractState.pop();
                    abstractState.setArrayElement(new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, value);
                    break;
                }
                case Instruction.OP_LASTORE:
                case Instruction.OP_DASTORE:
                {
                    TaintAbstractState value = abstractState.pop();
                    abstractState.pop();
                    TaintAbstractState index = abstractState.pop();
                    abstractState.pop();
                    abstractState.setArrayElement(new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, value);
                    break;
                }
                default:
                    super.visitSimpleInstruction(clazz, method, codeAttribute, offset, simpleInstruction);
            }
        }

        @Override
        public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
        {
            constantLookupVisitor.resetResult();
            switch (constantInstruction.opcode)
            {
                case Instruction.OP_GETFIELD:
                {
                    constantLookupVisitor.isStatic = false;
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
                    TaintAbstractState result = abstractState.getFieldOrDefault(new JvmStackLocation(constantInstruction.stackPopCount(clazz) - 1),
                                                                                constantLookupVisitor.result,
                                                                                abstractState.pop());
                    if (constantLookupVisitor.resultSize > 1)
                    {
                        abstractState.push(getAbstractDefault());
                    }
                    abstractState.push(result);
                    break;
                }
                case Instruction.OP_PUTFIELD:
                {
                    constantLookupVisitor.isStatic = false;
                    clazz.constantPoolEntryAccept(constantInstruction.constantIndex, constantLookupVisitor);
                    TaintAbstractState value = abstractState.pop();
                    if (constantLookupVisitor.resultSize > 1)
                    {
                        abstractState.pop();
                    }
                    abstractState.pop();
                    abstractState.setField(new JvmStackLocation(constantInstruction.stackPopCount(clazz) - 1), constantLookupVisitor.result, value);
                    break;
                }
                default:
                    super.visitConstantInstruction(clazz, method, codeAttribute, offset, constantInstruction);
            }
        }
    }
}
