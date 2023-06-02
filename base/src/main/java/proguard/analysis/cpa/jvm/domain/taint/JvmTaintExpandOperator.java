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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultExpandOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.util.HeapUtil;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;
import proguard.classfile.instruction.Instruction;

/**
 * This {@link proguard.analysis.cpa.bam.ExpandOperator} inherits all the functionalities of a {@link JvmDefaultExpandOperator} and in addition taints the return values if the called function
 * is a source.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmTaintExpandOperator
    extends JvmDefaultExpandOperator<SetAbstractState<JvmTaintSource>>
{

    private final Map<Signature, Set<JvmTaintSource>> signaturesToSources;

    /**
     * Create the operator specifying the taint sources.
     *
     * @param cfa                 the control flow automaton of the analyzed program.
     * @param signaturesToSources a mapping from method signatures to their {@link JvmTaintSource}
     * @param expandHeap          whether expansion of the heap is performed
     */
    public JvmTaintExpandOperator(JvmCfa cfa, Map<Signature, Set<JvmTaintSource>> signaturesToSources, boolean expandHeap)
    {
        super(cfa, expandHeap);
        this.signaturesToSources = signaturesToSources;
    }

    /**
     * Create the operator specifying the taint sources.
     *
     * @param cfa                 the control flow automaton of the analyzed program.
     * @param signaturesToSources a mapping from method signatures to their {@link JvmTaintSource}
     */
    public JvmTaintExpandOperator(JvmCfa cfa, Map<Signature, Set<JvmTaintSource>> signaturesToSources)
    {
        this(cfa, signaturesToSources, true);
    }

    // implementations for JvmDefaultExpandOperator

    @Override
    public JvmTaintAbstractState expand(AbstractState expandedInitialState, AbstractState reducedExitState, JvmCfaNode blockEntryNode, Call call)
    {
        JvmTaintAbstractState result = (JvmTaintAbstractState) super.expand(expandedInitialState, reducedExitState, blockEntryNode, call);
        List<JvmTaintSource> detectedSources = signaturesToSources.getOrDefault(call.getTarget(), Collections.emptySet())
                                                                  .stream()
                                                                  .filter(s -> s.callMatcher.map(m -> m.test(call)).orElse(true))
                                                                  .collect(Collectors.toList());
        if (detectedSources.isEmpty())
        {
            return result;
        }
        // taint static fields
        Map<String, SetAbstractState<JvmTaintSource>> fqnToValue = new HashMap<>();
        detectedSources.stream()
                       .filter(s -> !s.taintsGlobals.isEmpty())
                       .forEach(s ->
                                {
                                    SetAbstractState<JvmTaintSource> newValue = new SetAbstractState<>(s);
                                    s.taintsGlobals.forEach(fqn -> fqnToValue.merge(fqn, newValue, SetAbstractState::join));
                                });
        fqnToValue.forEach((fqn, value) -> result.setStatic(fqn, value, SetAbstractState.bottom));
        if (!(result.getHeap() instanceof JvmTaintTreeHeapFollowerAbstractState))
        {
            return result;
        }
        // taint heap
        JvmTaintTreeHeapFollowerAbstractState expandedHeap = (JvmTaintTreeHeapFollowerAbstractState) ((JvmTaintAbstractState) expandedInitialState).getHeap();
        // taint static references
        fqnToValue.forEach((key, value) -> result.setObjectTaint(expandedHeap.getReferenceAbstractState(new JvmStaticFieldLocation(key)), value));
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
        argToValue.forEach((a, value) -> result.setObjectTaint(HeapUtil.getArgumentReference(expandedHeap, parameterSize, descriptor, call.isStatic(), a - 1),
                                                               value));
        // taint the calling instance
        List<JvmTaintSource> sourcesTaintingThis = detectedSources.stream().filter(s -> s.taintsThis).collect(Collectors.toList());
        if (!sourcesTaintingThis.isEmpty())
        {
            result.setObjectTaint(expandedHeap.getReferenceAbstractState(new JvmStackLocation(parameterSize - 1)),
                                  new SetAbstractState<>(sourcesTaintingThis));
        }
        return result;
    }

    /**
     * The calculation of return values supports tainting it in case the analyzed method is a taint source.
     */
    @Override
    protected List<SetAbstractState<JvmTaintSource>> calculateReturnValues(AbstractState reducedExitState, Instruction returnInstruction, Call call)
    {
        List<JvmTaintSource> detectedSources = signaturesToSources.getOrDefault(call.getTarget(), Collections.emptySet())
                                                                  .stream()
                                                                  .filter(s -> s.callMatcher.map(m -> m.test(call)).orElse(true))
                                                                  .filter(s -> s.taintsReturn)
                                                                  .collect(Collectors.toList());

        if (detectedSources.isEmpty())
        {
            return super.calculateReturnValues(reducedExitState, returnInstruction, call);
        }

        List<SetAbstractState<JvmTaintSource>> returnValues = new ArrayList<>();

        SetAbstractState<JvmTaintSource> answerContent = new SetAbstractState<>(detectedSources);
        int returnSize = returnInstruction.stackPopCount(null);
        for (int i = 0; i < returnSize; i++)
        {
            SetAbstractState<JvmTaintSource> returnByte = ((JvmAbstractState<SetAbstractState<JvmTaintSource>>) reducedExitState).peek(i);
            answerContent = answerContent.join(returnByte);
        }

        // pad to meet the return type size and append the abstract state
        for (int i = returnSize; i > 1; i--)
        {
            returnValues.add(SetAbstractState.bottom);
        }
        if (returnSize > 0)
        {
            returnValues.add(answerContent);
        }

        return returnValues;
    }

    // implementations for JvmAbstractStateFactory

    @Override
    public JvmTaintAbstractState createJvmAbstractState(JvmCfaNode programLocation,
                                                        JvmFrameAbstractState<SetAbstractState<JvmTaintSource>> frame,
                                                        JvmHeapAbstractState<SetAbstractState<JvmTaintSource>> heap,
                                                        MapAbstractState<String, SetAbstractState<JvmTaintSource>> staticFields)
    {
        return new JvmTaintAbstractState(programLocation, frame, heap, staticFields);
    }

    /**
     * Returns the mapping from fqns to taint sources.
     */
    public Map<Signature, Set<JvmTaintSource>> getSignaturesToSources()
    {
        return signaturesToSources;
    }
}
