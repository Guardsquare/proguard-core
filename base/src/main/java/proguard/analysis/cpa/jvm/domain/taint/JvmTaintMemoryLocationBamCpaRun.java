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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.bam.BlockAbstraction;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.CallEdge;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.memory.BamLocationDependentJvmMemoryLocation;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationAbstractState;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationBamCpaRun;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationCpa;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.heap.HeapModel;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.state.HashMapAbstractStateFactory;
import proguard.analysis.cpa.state.MapAbstractStateFactory;
import proguard.analysis.cpa.util.StateNames;
import proguard.classfile.MethodSignature;

/**
 * This run wraps the execution of BAM {@link JvmMemoryLocationCpa}.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintMemoryLocationBamCpaRun
    extends JvmMemoryLocationBamCpaRun<SimpleCpa, TaintAbstractState>
{

    private final Collection<? extends JvmTaintSink>             taintSinks;
    private       List<BamLocationDependentJvmMemoryLocation<?>> endPoints;

    /**
     * Create a traced taint CPA run.
     *
     * @param jvmTaintCpaRun an intraprocedural taint CPA run
     * @param threshold      a cut-off threshold
     * @param taintSinks     a collection of taint sinks
     */
    protected JvmTaintMemoryLocationBamCpaRun(JvmTaintBamCpaRun jvmTaintCpaRun,
                                              TaintAbstractState threshold,
                                              Collection<? extends JvmTaintSink> taintSinks)
    {
        super(jvmTaintCpaRun, threshold, jvmTaintCpaRun.getAbortOperator());
        this.taintSinks = taintSinks;
    }

    /**
     * Create a traced taint CPA run.
     *
     * @param cfa                             a CFA
     * @param taintSources                    a set of taint sources
     * @param mainSignature                   the main signature of the main method
     * @param maxCallStackDepth               the maximum depth of the call stack analyzed interprocedurally.
     *                                        0 means intraprocedural analysis.
     *                                        < 0 means no maximum depth.
     * @param threshold                       a cut-off threshold
     * @param taintSinks                      a collection of taint sinks
     * @param abortOperator                   an abort operator
     * @param reduceHeap                      whether reduction/expansion of the heap state is performed at call/return sites
     * @param heapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    protected JvmTaintMemoryLocationBamCpaRun(JvmCfa cfa,
                                              Set<? extends TaintSource> taintSources,
                                              MethodSignature mainSignature,
                                              int maxCallStackDepth,
                                              HeapModel heapModel,
                                              TaintAbstractState threshold,
                                              Collection<? extends JvmTaintSink> taintSinks,
                                              AbortOperator abortOperator,
                                              boolean reduceHeap,
                                              MapAbstractStateFactory heapNodeMapAbstractStateFactory,
                                              MapAbstractStateFactory staticFieldMapAbstractStateFactory)
    {
        this(new JvmTaintBamCpaRun<JvmAbstractState<TaintAbstractState>>(cfa,
                                                                         taintSources,
                                                                         mainSignature,
                                                                         maxCallStackDepth,
                                                                         heapModel,
                                                                         abortOperator,
                                                                         reduceHeap,
                                                                         heapNodeMapAbstractStateFactory,
                                                                         staticFieldMapAbstractStateFactory),
             threshold,
             taintSinks);
    }

    // implementations for CpaRun

    @Override
    public List<JvmMemoryLocationAbstractState<?>> getInitialStates()
    {
        return getEndPoints().stream()
                             .map(JvmMemoryLocationAbstractState::new)
                             .collect(Collectors.toList());
    }

    // implementations for TraceExtractor

    @Override
    public Collection<BamLocationDependentJvmMemoryLocation<?>> getEndPoints()
    {
        List<BamLocationDependentJvmMemoryLocation<?>> memoryLocations = new ArrayList<>();
        Map<String, Set<JvmMemoryLocation>> fqnToLocations = JvmTaintSink.convertSinksToMemoryLocations(taintSinks);
        // if the end points have been already computed, return their cached set
        if (endPoints != null)
        {
            return endPoints;
        }
        // if the input reached set is missing, run the whole analysis
        // and return the end points calculated after {@code getInitialStates()}
        if (inputReachedSet == null)
        {
            execute();
            return endPoints;
        }

        // find reached taint sinks in all cached reached sets
        inputCpaRun.getCpa()
                   .getCache()
                   .values()
                   .stream()
                   .map(BlockAbstraction::getReachedSet)
                   .forEach(reachedSet -> reachedSet
                       .asCollection()
                       .stream()
                       .forEach(state -> ((JvmAbstractState<TaintAbstractState>) state.getStateByName(StateNames.Jvm))
                           .getProgramLocation()
                           .getLeavingEdges()
                           .stream()
                           .filter(e -> e instanceof CallEdge)
                           .map(e -> ((CallEdge) e).getCall().getTarget().getFqn())
                           .forEach(fqn -> fqnToLocations.getOrDefault(fqn, Collections.emptySet())
                                                         .stream()
                                                         .filter(l -> !l.extractValueOrDefault((JvmAbstractState<TaintAbstractState>) state.getStateByName(StateNames.Jvm),
                                                                                               TaintAbstractState.bottom)
                                                                        .isEmpty())
                                                         .forEach(l -> memoryLocations.add(
                                                             new BamLocationDependentJvmMemoryLocation(l,
                                                                                                       ((ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>) state).getProgramLocation(),
                                                                                                       (ProgramLocationDependentReachedSet) reachedSet))))));

        return endPoints = memoryLocations;
    }

    /**
     * A builder for {@link JvmTaintMemoryLocationBamCpaRun}. It assumes either the best performing parameters or the most basic one, if there is no absolute benefit.
     *
     * @author Dmitry Ivanov
     */
    public static class Builder
    {

        private JvmCfa                             cfa;
        private MethodSignature                    mainSignature;
        private Set<? extends TaintSource>         taintSources                       = Collections.emptySet();
        private int                                maxCallStackDepth                  = -1;
        private HeapModel                          heapModel                          = HeapModel.FORGETFUL;
        private TaintAbstractState                 threshold                          = TaintAbstractState.bottom;
        private Collection<? extends JvmTaintSink> taintSinks                         = Collections.emptySet();
        private AbortOperator                      abortOperator                      = NeverAbortOperator.INSTANCE;
        private boolean                            reduceHeap                         = true;
        private MapAbstractStateFactory            heapNodeMapAbstractStateFactory    = HashMapAbstractStateFactory.INSTANCE;;
        private MapAbstractStateFactory            staticFieldMapAbstractStateFactory = HashMapAbstractStateFactory.INSTANCE;;

        /**
         * Returns the {@link JvmTaintMemoryLocationBamCpaRun} for given parameters.
         */
        public JvmTaintMemoryLocationBamCpaRun build()
        {
            if (cfa == null || mainSignature == null)
            {
                throw new IllegalStateException("CFA and the main signature must be set");
            }
            return new JvmTaintMemoryLocationBamCpaRun(cfa,
                                                       taintSources,
                                                       mainSignature,
                                                       maxCallStackDepth,
                                                       heapModel,
                                                       threshold,
                                                       taintSinks,
                                                       abortOperator,
                                                       reduceHeap,
                                                       heapNodeMapAbstractStateFactory,
                                                       staticFieldMapAbstractStateFactory);
        }

        /**
         * Sets the control flow automaton.
         */
        public Builder setCfa(JvmCfa cfa)
        {
            this.cfa = cfa;
            return this;
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
         * Sets the call stack limit for the interprocedural analysis.
         */
        public Builder setMaxCallStackDepth(int maxCallStackDepth)
        {
            this.maxCallStackDepth = maxCallStackDepth;
            return this;
        }

        /**
         * Sets the heap model.
         */
        public Builder setHeapModel(HeapModel heapModel)
        {
            this.heapModel = heapModel;
            return this;
        }

        /**
         * Sets the trace reconstruction threshold.
         */
        public Builder setThreshold(TaintAbstractState threshold)
        {
            this.threshold = threshold;
            return this;
        }

        /**
         * Sets the taint sinks.
         */
        public Builder setTaintSinks(Collection<? extends JvmTaintSink> taintSinks)
        {
            this.taintSinks = taintSinks;
            return this;
        }

        /**
         * Sets the abort operator for premature CPA algorithm termination.
         */
        public Builder setAbortOperator(AbortOperator abortOperator)
        {
            this.abortOperator = abortOperator;
            return this;
        }

        /**
         * Sets whether the heap should be reduced before method calls.
         */
        public Builder setReduceHeap(boolean reduceHeap)
        {
            this.reduceHeap = reduceHeap;
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
