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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.bam.CpaWithBamOperators;
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.CompositeHeapJvmAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.CompositeHeapTransferRelation;
import proguard.analysis.cpa.jvm.domain.reference.JvmCompositeHeapExpandOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceExpandOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceTransferRelation;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.HeapModel;
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapPrincipalAbstractState;
import proguard.analysis.cpa.jvm.util.JvmBamCpaRun;

/**
 * This run wraps the execution of BAM {@link JvmTaintCpa}.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintBamCpaRun<OuterAbstractStateT extends AbstractState>
    extends JvmBamCpaRun<SimpleCpa, TaintAbstractState, OuterAbstractStateT>
{

    private final Set<TaintSource> taintSources;
    private final MethodSignature  mainMethodSignature;

    /**
     * Create a CPA run.
     *
     * @param cfa                 a CFA
     * @param taintSources        a set of taint sources
     * @param mainMethodSignature the signature of the main method
     * @param maxCallStackDepth   maximum depth of the call stack analyzed inter-procedurally.
     *                            0 means intra-procedural analysis.
     *                            < 0 means no maximum depth.
     * @param heapModel           a heap model to be used
     */
    public JvmTaintBamCpaRun(JvmCfa cfa, Set<TaintSource> taintSources, MethodSignature mainMethodSignature, int maxCallStackDepth, HeapModel heapModel)
    {
        super(cfa, maxCallStackDepth, heapModel);
        this.taintSources = taintSources;
        this.mainMethodSignature = mainMethodSignature;
    }

    /**
     * Create a CPA run with a forgetful heap model.
     *
     * @param cfa                 a CFA
     * @param taintSources        a set of taint sources
     * @param mainMethodSignature the signature of the main method
     * @param maxCallStackDepth   maximum depth of the call stack analyzed inter-procedurally.
     *                            0 means intra-procedural analysis.
     *                            < 0 means no maximum depth.
     */
    public JvmTaintBamCpaRun(JvmCfa cfa, Set<TaintSource> taintSources, MethodSignature mainMethodSignature, int maxCallStackDepth)
    {
        this(cfa, taintSources, mainMethodSignature, maxCallStackDepth, HeapModel.FORGETFUL);
    }

    // implementations for JvmBamCpaRun

    @Override
    public SimpleCpa createIntraproceduralCPA()
    {
        switch (heapModel)
        {
            case FORGETFUL:
                return new JvmTaintCpa(taintSources);
            case TREE:
                AbstractDomain abstractDomain = new DelegateAbstractDomain<CompositeHeapJvmAbstractState>();
                return new SimpleCpa(abstractDomain,
                                     new CompositeHeapTransferRelation(Arrays.asList(new JvmReferenceTransferRelation(),
                                                                                     new JvmTaintTransferRelation(JvmTaintCpa.createSourcesMap(taintSources)))),
                                     new MergeJoinOperator(abstractDomain),
                                     new StopJoinOperator(abstractDomain));
            default:
                throw new IllegalArgumentException("Heap model " + heapModel.name() + " is not supported by " + getClass().getName());
        }
    }

    @Override
    public ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createExpandOperator()
    {
        switch (heapModel)
        {
            case FORGETFUL:
                return new JvmTaintExpandOperator(cfa, JvmTaintCpa.createSourcesMap(taintSources));
            case TREE:
                return new JvmCompositeHeapExpandOperator(Arrays.asList(new JvmReferenceExpandOperator(cfa), new JvmTaintExpandOperator(cfa, JvmTaintCpa.createSourcesMap(taintSources))));
            default:
                throw new IllegalArgumentException("Heap model " + heapModel.name() + " is not supported by " + getClass().getName());
        }
    }

    @Override
    public MethodSignature getMainSignature()
    {
        return mainMethodSignature;
    }

    @Override
    public Collection<OuterAbstractStateT> getInitialStates()
    {
        JvmFrameAbstractState<TaintAbstractState> emptyframe = new JvmFrameAbstractState<>();
        MapAbstractState<String, TaintAbstractState> emptyStaticFields = new MapAbstractState<>();
        JvmHeapAbstractState<TaintAbstractState> emptyHeap;
        switch (heapModel)
        {
            case FORGETFUL:
                emptyHeap = new JvmForgetfulHeapAbstractState<>(TaintAbstractState.bottom);
                return Collections.singleton((OuterAbstractStateT) new JvmAbstractState<>(cfa.getFunctionEntryNode(mainMethodSignature),
                                                                                          emptyframe,
                                                                                          emptyHeap,
                                                                                          emptyStaticFields));
            case TREE:
                JvmReferenceAbstractState principalState = new JvmReferenceAbstractState(cfa.getFunctionEntryNode(mainMethodSignature),
                                                                                         new JvmFrameAbstractState<>(),
                                                                                         new JvmTreeHeapPrincipalAbstractState(),
                                                                                         new MapAbstractState<>());
                emptyHeap = new JvmTreeHeapFollowerAbstractState<>(principalState, TaintAbstractState.bottom);
                return (Collection<OuterAbstractStateT>) Collections.singleton(new CompositeHeapJvmAbstractState(Arrays.asList(principalState,
                                                                                                                               new JvmAbstractState<>(cfa.getFunctionEntryNode(mainMethodSignature),
                                                                                                                                                      emptyframe,
                                                                                                                                                      emptyHeap,
                                                                                                                                                      emptyStaticFields))));
            default:
                throw new IllegalStateException("Invalid heap model: " + heapModel.name());
        }
    }

    // implementations for BamCpaRun

    @Override
    public BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> getCpa()
    {
        return cpa == null
               ? heapModel == HeapModel.FORGETFUL
                 ? super.getCpa()
                 : new BamCpa<>(new CpaWithBamOperators<>(createIntraproceduralCPA(),
                                                          createReduceOperator(),
                                                          createExpandOperator(),
                                                          createRebuildOperator()),
                                getCfa(),
                                getMainSignature(),
                                createCache(),
                                maxCallStackDepth)
               : cpa;
    }
}
