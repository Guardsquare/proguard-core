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
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.MergeJoinOperator;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopJoinOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.CompositeHeapJvmAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.CompositeHeapTransferRelation;
import proguard.analysis.cpa.jvm.domain.reference.JvmCompositeHeapExpandOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmCompositeHeapReduceOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceExpandOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceReduceOperator;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceTransferRelation;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.HeapModel;
import proguard.analysis.cpa.jvm.state.heap.JvmForgetfulHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.HeapNode;
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
    extends JvmBamCpaRun<SimpleCpa, SetAbstractState<JvmTaintSource>, OuterAbstractStateT>
{

    private final Set<? extends JvmTaintSource>                                                     taintSources;
    private final MethodSignature                                                                mainMethodSignature;
    private final MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>              staticFieldMapAbstractStateFactory;
    private final MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>>      principalHeapMapAbstractStateFactory;
    private final MapAbstractStateFactory<String, SetAbstractState<Reference>>                   principalHeapNodeMapAbstractStateFactory;
    private final MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> followerHeapMapAbstractStateFactory;
    private final MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>              followerHeapNodeMapAbstractStateFactory;

    /**
     * Create a CPA run. If reduceHeap is set to false no reduction/expansion is applied to the heap states at call/return sites
     * (this parameter is irrelevant for FORGETFUL heap model).
     *
     * @param cfa                                      a CFA
     * @param taintSources                             a set of taint sources
     * @param mainMethodSignature                      the signature of the main method
     * @param maxCallStackDepth                        the maximum depth of the call stack analyzed interprocedurally
     *                                                 0 means intraprocedural analysis
     *                                                 < 0 means no maximum depth
     * @param heapModel                                a heap model to be used
     * @param abortOperator                            an abort operator
     * @param reduceHeap                               whether reduction/expansion of the heap state is performed at call/return sites
     * @param principalHeapMapAbstractStateFactory     a map abstract state factory used for constructing the mapping from references to objects in the principal heap model
     * @param principalHeapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values in the principal heap model
     * @param followerHeapMapAbstractStateFactory      a map abstract state factory used for constructing the mapping from references to objects in the follower heap model
     * @param followerHeapNodeMapAbstractStateFactory  a map abstract state factory used for constructing the mapping from fields to values in the follower heap model
     */
    protected JvmTaintBamCpaRun(JvmCfa cfa,
                                Set<? extends JvmTaintSource> taintSources,
                                MethodSignature mainMethodSignature,
                                int maxCallStackDepth,
                                HeapModel heapModel,
                                AbortOperator abortOperator,
                                boolean reduceHeap,
                                MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> staticFieldMapAbstractStateFactory,
                                MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>> principalHeapMapAbstractStateFactory,
                                MapAbstractStateFactory<String, SetAbstractState<Reference>> principalHeapNodeMapAbstractStateFactory,
                                MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> followerHeapMapAbstractStateFactory,
                                MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> followerHeapNodeMapAbstractStateFactory)
    {
        super(cfa, maxCallStackDepth, heapModel, abortOperator, reduceHeap);
        this.taintSources                             = taintSources;
        this.mainMethodSignature                      = mainMethodSignature;
        this.staticFieldMapAbstractStateFactory       = staticFieldMapAbstractStateFactory;
        this.principalHeapMapAbstractStateFactory     = principalHeapMapAbstractStateFactory;
        this.principalHeapNodeMapAbstractStateFactory = principalHeapNodeMapAbstractStateFactory;
        this.followerHeapMapAbstractStateFactory      = followerHeapMapAbstractStateFactory;
        this.followerHeapNodeMapAbstractStateFactory  = followerHeapNodeMapAbstractStateFactory;

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
            case TAINT_TREE:
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
    public ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature> createReduceOperator()
    {
        switch (heapModel)
        {
            case TREE:
            case TAINT_TREE:
                return new JvmCompositeHeapReduceOperator(Arrays.asList(new JvmReferenceReduceOperator(reduceHeap),
                                                                        new JvmTaintReduceOperator(reduceHeap)));
            default:
                return super.createReduceOperator();
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
            case TAINT_TREE:
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
                                                                                          new JvmForgetfulHeapAbstractState<>(SetAbstractState.bottom),
                                                                                          staticFieldMapAbstractStateFactory.createMapAbstractState()));
            case TREE:
            case TAINT_TREE:
                JvmReferenceAbstractState principalState = new JvmReferenceAbstractState(cfa.getFunctionEntryNode(mainMethodSignature),
                                                                                         new JvmFrameAbstractState<>(),
                                                                                         new JvmTreeHeapPrincipalAbstractState(principalHeapMapAbstractStateFactory,
                                                                                                                               principalHeapNodeMapAbstractStateFactory),
                                                                                         principalHeapNodeMapAbstractStateFactory.createMapAbstractState());
                return (Collection<OuterAbstractStateT>) Collections.singleton(new CompositeHeapJvmAbstractState(Arrays.asList(
                    principalState,
                    new JvmTaintAbstractState(cfa.getFunctionEntryNode(mainMethodSignature),
                                              new JvmFrameAbstractState<>(),
                                              heapModel == HeapModel.TAINT_TREE
                                              ? new JvmTaintTreeHeapFollowerAbstractState(principalState,
                                                                                          SetAbstractState.bottom,
                                                                                          followerHeapMapAbstractStateFactory.createMapAbstractState(),
                                                                                          followerHeapMapAbstractStateFactory,
                                                                                          followerHeapNodeMapAbstractStateFactory)
                                              : new JvmBasicTaintTreeHeapFollowerAbstractState(principalState,
                                                                                               SetAbstractState.bottom,
                                                                                               followerHeapMapAbstractStateFactory.createMapAbstractState(),
                                                                                               followerHeapMapAbstractStateFactory,
                                                                                               followerHeapNodeMapAbstractStateFactory),
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

        protected MethodSignature                                                                mainSignature;
        protected Set<? extends JvmTaintSource>                                                  taintSources                             = Collections.emptySet();
        protected MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>              staticFieldMapAbstractStateFactory       = HashMapAbstractStateFactory.getInstance();
        protected MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>>      principalHeapMapAbstractStateFactory     = HashMapAbstractStateFactory.getInstance();
        protected MapAbstractStateFactory<String, SetAbstractState<Reference>>                   principalHeapNodeMapAbstractStateFactory = HashMapAbstractStateFactory.getInstance();
        protected MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> followerHeapMapAbstractStateFactory      = HashMapAbstractStateFactory.getInstance();
        protected MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>              followerHeapNodeMapAbstractStateFactory  = HashMapAbstractStateFactory.getInstance();

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
                                           staticFieldMapAbstractStateFactory,
                                           principalHeapMapAbstractStateFactory,
                                           principalHeapNodeMapAbstractStateFactory,
                                           followerHeapMapAbstractStateFactory,
                                           followerHeapNodeMapAbstractStateFactory);
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
        public Builder setTaintSources(Set<? extends JvmTaintSource> taintSources)
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
         * Sets the static field map abstract state factory.
         */
        public Builder setStaticFieldMapAbstractStateFactory(MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> staticFieldMapAbstractStateFactory)
        {
            this.staticFieldMapAbstractStateFactory = staticFieldMapAbstractStateFactory;
            return this;
        }

        /**
         * Sets the map abstract state factory used for constructing the mapping from references to objects in the principal heap model.
         */
        public Builder setPrincipalHeapMapAbstractStateFactory(MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>> principalHeapMapAbstractStateFactory)
        {
            this.principalHeapMapAbstractStateFactory = principalHeapMapAbstractStateFactory;
            return this;
        }

        /**
         * Sets the map abstract state factory used for constructing the mapping from fields to values in the principal heap model.
         */
        public Builder setPrincipalHeapNodeMapAbstractStateFactory(MapAbstractStateFactory<String, SetAbstractState<Reference>> principalHeapNodeMapAbstractStateFactory)
        {
            this.principalHeapNodeMapAbstractStateFactory = principalHeapNodeMapAbstractStateFactory;
            return this;
        }

        /**
         * Sets the map abstract state factory used for constructing the mapping from references to objects in the follower heap model.
         */
        public Builder setFollowerHeapMapAbstractStateFactory(MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> followerHeapMapAbstractStateFactory)
        {
            this.followerHeapMapAbstractStateFactory = followerHeapMapAbstractStateFactory;
            return this;
        }

        /**
         * Sets the map abstract state factory used for constructing the mapping from fields to values in the follower heap model.
         */
        public Builder setFollowerHeapNodeMapAbstractStateFactory(MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> followerHeapNodeMapAbstractStateFactory)
        {
            this.followerHeapNodeMapAbstractStateFactory = followerHeapNodeMapAbstractStateFactory;
            return this;
        }
    }
}
