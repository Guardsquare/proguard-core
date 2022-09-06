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
import java.util.List;
import java.util.Map;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
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
import proguard.classfile.instruction.Instruction;
import proguard.classfile.util.ClassUtil;

/**
 * This {@link proguard.analysis.cpa.bam.ExpandOperator} inherits all the functionalities of a {@link JvmDefaultExpandOperator} and in addition taints the return values if the called function
 * is a source.
 *
 * @author Carlo Alberto Pozzoli
 */
public class JvmTaintExpandOperator
    extends JvmDefaultExpandOperator<TaintAbstractState>
{

    private final Map<String, TaintSource> fqnToSources;

    /**
     * Create the operator specifying the taint sources.
     *
     * @param cfa          the control flow automaton of the analyzed program.
     * @param fqnToSources a mapping from fully qualified names of methods to their {@link TaintSource}
     * @param expandHeap   whether expansion of the heap is performed
     */
    public JvmTaintExpandOperator(JvmCfa cfa, Map<String, TaintSource> fqnToSources, boolean expandHeap)
    {
        super(cfa, expandHeap);
        this.fqnToSources = fqnToSources;
    }

    /**
     * Create the operator specifying the taint sources.
     *
     * @param cfa          the control flow automaton of the analyzed program.
     * @param fqnToSources a mapping from fully qualified names of methods to their {@link TaintSource}
     */
    public JvmTaintExpandOperator(JvmCfa cfa, Map<String, TaintSource> fqnToSources)
    {
        this(cfa, fqnToSources, true);
    }

    // implementations for JvmDefaultExpandOperator

    @Override
    public JvmTaintAbstractState expand(AbstractState expandedInitialState, AbstractState reducedExitState, JvmCfaNode blockEntryNode, Call call)
    {
        JvmTaintAbstractState result = (JvmTaintAbstractState) super.expand(expandedInitialState, reducedExitState, blockEntryNode, call);
        TaintSource detectedSource = fqnToSources.get(call.getTarget().getFqn());
        if (detectedSource == null)
        {
            return result;
        }
        TaintAbstractState detectedTaint = new TaintAbstractState(detectedSource);
        // taint static fields
        detectedSource.taintsGlobals.forEach(g -> result.setStatic(g, detectedTaint));
        if (!(result.getHeap() instanceof JvmTaintTreeHeapFollowerAbstractState))
        {
            return result;
        }
        // taint heap
        JvmTaintTreeHeapFollowerAbstractState expandedHeap = (JvmTaintTreeHeapFollowerAbstractState) ((JvmTaintAbstractState) expandedInitialState).getHeap();
        // taint static references
        detectedSource.taintsGlobals.stream()
                                    .map(JvmStaticFieldLocation::new)
                                    .map(expandedHeap::getReferenceAbstractState)
                                    .forEach(r -> result.setObjectTaint(r, detectedTaint));
        if (detectedSource.taintsArgs.isEmpty() && !detectedSource.taintsThis)
        {
            return result;
        }
        boolean isStatic = call.invocationOpcode == Instruction.OP_INVOKESTATIC || call.invocationOpcode == Instruction.OP_INVOKEDYNAMIC;
        String descriptor = call.getTarget().descriptor.toString();
        int parameterSize = ClassUtil.internalMethodParameterSize(descriptor, isStatic);
        // taint arguments
        detectedSource.taintsArgs.stream()
                                 .map(a -> HeapUtil.getArgumentReference(expandedHeap, parameterSize, descriptor, isStatic, a - 1))
                                 .forEach(r -> result.setObjectTaint(r, detectedTaint));
        // taint the calling instance
        if (detectedSource.taintsThis)
        {
            result.setObjectTaint(expandedHeap.getReferenceAbstractState(new JvmStackLocation(parameterSize - 1)),
                                  detectedTaint);
        }
        return result;
    }

    /**
     * The calculation of return values supports tainting it in case the analyzed method is a taint source.
     */
    @Override
    protected List<TaintAbstractState> calculateReturnValues(AbstractState reducedExitState, Instruction returnInstruction, Call call)
    {
        TaintSource detectedSource = fqnToSources.get(call.getTarget().getFqn());

        if (detectedSource == null || !detectedSource.taintsReturn)
        {
            return super.calculateReturnValues(reducedExitState, returnInstruction, call);
        }

        List<TaintAbstractState> returnValues = new ArrayList<>();

        TaintAbstractState answerContent = new TaintAbstractState();
        answerContent.add(detectedSource);
        int returnSize = returnInstruction.stackPopCount(null);
        for (int i = 0; i < returnSize; i++)
        {
            TaintAbstractState returnByte = ((JvmAbstractState<TaintAbstractState>) reducedExitState).peek(i);
            answerContent = answerContent.join(returnByte);
        }

        // pad to meet the return type size and append the abstract state
        for (int i = returnSize; i > 1; i--)
        {
            returnValues.add(TaintAbstractState.bottom);
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
                                                        JvmFrameAbstractState<TaintAbstractState> frame,
                                                        JvmHeapAbstractState<TaintAbstractState> heap,
                                                        MapAbstractState<String, TaintAbstractState> staticFields)
    {
        return new JvmTaintAbstractState(programLocation, frame, heap, staticFields);
    }

    /**
     * Returns the mapping from fqns to taint sources.
     */
    public Map<String, TaintSource> getFqnToSources()
    {
        return fqnToSources;
    }
}
