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
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.AbortOperator;
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
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapPrincipalAbstractState;
import proguard.analysis.cpa.jvm.util.JvmBamCpaRun;
import proguard.analysis.cpa.state.HashMapAbstractStateFactory;
import proguard.analysis.cpa.state.MapAbstractStateFactory;
import proguard.classfile.MethodSignature;

/**
 * This run wraps the execution of BAM {@link JvmTaintCpa}.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintBamCpaRun<OuterAbstractStateT extends AbstractState>
    extends JvmBamCpaRun<SimpleCpa, TaintAbstractState, OuterAbstractStateT>
{

    private final Set<? extends TaintSource> taintSources;
    private final MethodSignature            mainMethodSignature;
    private final MapAbstractStateFactory    heapNodeMapAbstractStateFactory;
    private final MapAbstractStateFactory    staticFieldMapAbstractStateFactory;

    /**
     * Create a CPA run. If reduceHeap is set to false no reduction/expansion is applied to the heap states at call/return sites
     * (this parameter is irrelevant for FORGETFUL heap model).
     *
     * @param cfa                             a CFA
     * @param taintSources                    a set of taint sources
     * @param mainMethodSignature             the signature of the main method
     * @param maxCallStackDepth               the maximum depth of the call stack analyzed interprocedurally
     *                                        0 means intraprocedural analysis
     *                                        < 0 means no maximum depth
     * @param heapModel                       a heap model to be used
     * @param abortOperator                   an abort operator
     * @param reduceHeap                      whether reduction/expansion of the heap state is performed at call/return sites
     * @param heapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    protected JvmTaintBamCpaRun(JvmCfa cfa,
                                Set<? extends TaintSource> taintSources,
                                MethodSignature mainMethodSignature,
                                int maxCallStackDepth,
                                HeapModel heapModel,
                                AbortOperator abortOperator,
                                boolean reduceHeap,
                                MapAbstractStateFactory heapNodeMapAbstractStateFactory,
                                MapAbstractStateFactory staticFieldMapAbstractStateFactory)
    {
        super(cfa, maxCallStackDepth, heapModel, abortOperator, reduceHeap);
        this.taintSources                       = taintSources;
        this.mainMethodSignature                = mainMethodSignature;
        this.heapNodeMapAbstractStateFactory    = heapNodeMapAbstractStateFactory;
        this.staticFieldMapAbstractStateFactory = staticFieldMapAbstractStateFactory;
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
        JvmTaintExpandOperator jvmExpandOperator = new JvmTaintExpandOperator(cfa, JvmTaintCpa.createSourcesMap(taintSources), reduceHeap);

        switch (heapModel)
        {
            case FORGETFUL:
                return jvmExpandOperator;
            case TREE:
                return new JvmCompositeHeapExpandOperator(Arrays.asList(new JvmReferenceExpandOperator(cfa, reduceHeap), jvmExpandOperator));
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
        switch (heapModel)
        {
            case FORGETFUL:
                return Collections.singleton((OuterAbstractStateT) new JvmAbstractState<>(cfa.getFunctionEntryNode(mainMethodSignature),
                                                                                          new JvmFrameAbstractState<>(),
                                                                                          new JvmForgetfulHeapAbstractState<>(TaintAbstractState.bottom),
                                                                                          staticFieldMapAbstractStateFactory.createMapAbstractState()));
            case TREE:
                JvmReferenceAbstractState principalState = new JvmReferenceAbstractState(cfa.getFunctionEntryNode(mainMethodSignature),
                                                                                         new JvmFrameAbstractState<>(),
                                                                                         new JvmTreeHeapPrincipalAbstractState(heapNodeMapAbstractStateFactory),
                                                                                         staticFieldMapAbstractStateFactory.createMapAbstractState());
                return (Collection<OuterAbstractStateT>) Collections.singleton(new CompositeHeapJvmAbstractState(Arrays.asList(
                    principalState,
                    new JvmAbstractState<>(cfa.getFunctionEntryNode(mainMethodSignature),
                                           new JvmFrameAbstractState<>(),
                                           new JvmTreeHeapFollowerAbstractState<>(principalState,
                                                                                  TaintAbstractState.bottom,
                                                                                  heapNodeMapAbstractStateFactory.createMapAbstractState(),
                                                                                  heapNodeMapAbstractStateFactory),
                                           staticFieldMapAbstractStateFactory.createMapAbstractState()))));
            default:
                throw new IllegalStateException("Invalid heap model: " + heapModel.name());
        }
    }

    /**
     * A builder for {@link JvmTaintBamCpaRun}. It assumes either the best performing parameters or the most basic one, if there is no absolute benefit.
     *
     * @author Dmitry Ivanov
     */
    public static class Builder extends JvmBamCpaRun.Builder
    {

        protected MethodSignature                    mainSignature;
        protected Set<? extends TaintSource>         taintSources                       = Collections.emptySet();
        protected MapAbstractStateFactory            heapNodeMapAbstractStateFactory    = HashMapAbstractStateFactory.INSTANCE;
        protected MapAbstractStateFactory            staticFieldMapAbstractStateFactory = HashMapAbstractStateFactory.INSTANCE;

        // implementations for JvmBamCpaRun.Builder

        /**
         * Returns the {@link JvmTaintBamCpaRun} for given parameters.
         */
        @Override
        public JvmTaintBamCpaRun<?> build()
        {
            if (cfa == null || mainSignature == null)
            {
                throw new IllegalStateException("CFA and the main signature must be set");
            }
            return new JvmTaintBamCpaRun<>(cfa,
                                           taintSources,
                                           mainSignature,
                                           maxCallStackDepth,
                                           heapModel,
                                           abortOperator,
                                           reduceHeap,
                                           heapNodeMapAbstractStateFactory,
                                           staticFieldMapAbstractStateFactory);
        }

        @Override
        public Builder setMaxCallStackDepth(int maxCallStackDepth)
        {
            return (Builder) super.setMaxCallStackDepth(maxCallStackDepth);
        }

        @Override
        public Builder setAbortOperator(AbortOperator abortOperator)
        {
            return (Builder) super.setAbortOperator(abortOperator);
        }

        @Override
        public Builder setReduceHeap(boolean reduceHeap)
        {
            return (Builder) super.setReduceHeap(reduceHeap);
        }

        @Override
        public Builder setCfa(JvmCfa cfa)
        {
            return (Builder) super.setCfa(cfa);
        }

        @Override
        public Builder setHeapModel(HeapModel heapModel)
        {
            return (Builder) super.setHeapModel(heapModel);
        }

        /**
         * Sets the taint sources.
         */
        public Builder setTaintSources(Set<? extends TaintSource> taintSources)
        {
            this.taintSources = taintSources;
            return this;
        }

        /**
         * Sets the signature of the method the analysis starts from.
         */
        public Builder setMainSignature(MethodSignature mainSignature)
        {
            this.mainSignature = mainSignature;
            return this;
        }

        /**
         * Sets the heap node abstract state factory.
         */
        public Builder setHeapNodeMapAbstractStateFactory(MapAbstractStateFactory heapNodeMapAbstractStateFactory)
        {
            this.heapNodeMapAbstractStateFactory = heapNodeMapAbstractStateFactory;
            return this;
        }

        /**
         * Sets the static field map abstract state factory.
         */
        public Builder setStaticFieldMapAbstractStateFactory(MapAbstractStateFactory staticFieldMapAbstractStateFactory)
        {
            this.staticFieldMapAbstractStateFactory = staticFieldMapAbstractStateFactory;
            return this;
        }
    }
}
