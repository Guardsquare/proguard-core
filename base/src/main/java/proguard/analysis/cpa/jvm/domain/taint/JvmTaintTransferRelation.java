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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.SetAbstractState;
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
import proguard.classfile.Signature;
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
    extends JvmTransferRelation<SetAbstractState<JvmTaintSource>>
{
    private final Map<Signature, Set<JvmTaintSource>> taintSources;

    /**
     * Create a taint transfer relation.
     *
     * @param taintSources a mapping from fully qualified names to taint sources
     */
    public JvmTaintTransferRelation(Map<Signature, Set<JvmTaintSource>> taintSources)
    {
        this.taintSources = taintSources;
    }

    // implementations for JvmTransferRelation

    @Override
    public void invokeMethod(JvmAbstractState<SetAbstractState<JvmTaintSource>> state, Call call, List<SetAbstractState<JvmTaintSource>> operands)
    {
        MethodSignature target = call.getTarget();
        List<JvmTaintSource> detectedSources = taintSources.getOrDefault(target, Collections.emptySet())
                                                           .stream()
                                                           .filter(s -> s.callMatcher.map(m -> m.test(call)).orElse(true))
                                                           .collect(Collectors.toList());
        int pushCount = ClassUtil.internalTypeSize(target.descriptor.returnType == null ? "?" : target.descriptor.returnType);
        SetAbstractState<JvmTaintSource> answerContent = operands.stream().reduce(getAbstractDefault(), SetAbstractState::join);

        // taint the return
        List<JvmTaintSource> detectedReturnSources = detectedSources.stream()
                                                                    .filter(s -> s.taintsReturn)
                                                                    .collect(Collectors.toList());
        if (!detectedReturnSources.isEmpty() && !answerContent.containsAll(detectedReturnSources))
        {
            answerContent = answerContent.copy();
            answerContent.addAll(detectedReturnSources);
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
        if (detectedSources.isEmpty())
        {
            return;
        }
        Map<String, SetAbstractState<JvmTaintSource>> fqnToValue = new HashMap<>();
        detectedSources.stream()
                       .filter(s -> !s.taintsGlobals.isEmpty())
                       .forEach(s ->
                                {
                                    SetAbstractState<JvmTaintSource> newValue = new SetAbstractState<>(s);
                                    s.taintsGlobals.forEach(fqn -> fqnToValue.merge(fqn, newValue, SetAbstractState::join));
                                });
        fqnToValue.forEach((fqn, value) -> state.setStatic(fqn, value, getAbstractDefault()));

        // taint heap
        if (!(state.getHeap() instanceof JvmTaintTreeHeapFollowerAbstractState))
        {
            return;
        }
        JvmTaintAbstractState taintAbstractState = (JvmTaintAbstractState) state;
        JvmTaintTreeHeapFollowerAbstractState treeHeap = (JvmTaintTreeHeapFollowerAbstractState) taintAbstractState.getHeap();
        // taint static fields
        fqnToValue.forEach((key, value) -> taintAbstractState.setObjectTaint(treeHeap.getReferenceAbstractState(new JvmStaticFieldLocation(key)), value));
        // taint arguments
        String descriptor = call.getTarget().descriptor.toString();
        int parameterSize = call.getJvmArgumentSize();
        Map<Integer, SetAbstractState<JvmTaintSource>> argToValue = new HashMap<>();
        detectedSources.stream()
                       .filter(s -> !s.taintsArgs.isEmpty())
                       .forEach(s ->
                                {
                                    SetAbstractState<JvmTaintSource> newValue = new SetAbstractState<>(s);
                                    s.taintsArgs.forEach(a -> argToValue.merge(a, newValue, SetAbstractState::join));
                                });
        argToValue.forEach((a, value) -> taintAbstractState.setObjectTaint(HeapUtil.getArgumentReference(treeHeap, parameterSize, descriptor, call.isStatic(), a - 1),
                                                                           value));
        // taint the calling instance
        List<JvmTaintSource> sourcesTaintingThis = detectedSources.stream().filter(s -> s.taintsThis).collect(Collectors.toList());
        if (!sourcesTaintingThis.isEmpty())
        {
            taintAbstractState.setObjectTaint(treeHeap.getReferenceAbstractState(new JvmStackLocation(parameterSize - 1)),
                                              new SetAbstractState<>(sourcesTaintingThis));
        }
    }

    @Override
    public SetAbstractState<JvmTaintSource> getAbstractDefault()
    {
        return SetAbstractState.bottom;
    }

    @Override
    protected JvmAbstractState<SetAbstractState<JvmTaintSource>> getAbstractSuccessorForInstruction(JvmAbstractState<SetAbstractState<JvmTaintSource>> abstractState,
                                                                                                    Instruction instruction,
                                                                                                    Clazz clazz,
                                                                                                    Precision precision)
    {
        instruction.accept(clazz, null, null, 0, new InstructionAbstractInterpreter(abstractState));
        return abstractState;
    }

    protected class InstructionAbstractInterpreter extends JvmTransferRelation<SetAbstractState<JvmTaintSource>>.InstructionAbstractInterpreter
    {

        public InstructionAbstractInterpreter(JvmAbstractState<SetAbstractState<JvmTaintSource>> abstractState)
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
                    SetAbstractState<JvmTaintSource> index = abstractState.pop();
                    abstractState.push(abstractState.getArrayElementOrDefault(new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, abstractState.pop()));
                    break;
                }
                case Instruction.OP_LALOAD:
                case Instruction.OP_DALOAD:
                {
                    SetAbstractState<JvmTaintSource> index = abstractState.pop();
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
                    SetAbstractState<JvmTaintSource> value = abstractState.pop();
                    SetAbstractState<JvmTaintSource> index = abstractState.pop();
                    abstractState.pop();
                    abstractState.setArrayElement(new JvmStackLocation(simpleInstruction.stackPopCount(clazz) - 1), index, value);
                    break;
                }
                case Instruction.OP_LASTORE:
                case Instruction.OP_DASTORE:
                {
                    SetAbstractState<JvmTaintSource> value = abstractState.pop();
                    abstractState.pop();
                    SetAbstractState<JvmTaintSource> index = abstractState.pop();
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
                    SetAbstractState<JvmTaintSource> result = abstractState.getFieldOrDefault(new JvmStackLocation(constantInstruction.stackPopCount(clazz) - 1),
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
                    SetAbstractState<JvmTaintSource> value = abstractState.pop();
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
