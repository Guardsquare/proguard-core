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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.bam.BlockAbstraction;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.memory.BamLocationDependentJvmMemoryLocation;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationAbstractState;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationBamCpaRun;
import proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationCpa;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.heap.HeapModel;
import proguard.analysis.cpa.jvm.state.heap.tree.HeapNode;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.state.HashMapAbstractStateFactory;
import proguard.analysis.cpa.state.MapAbstractStateFactory;
import proguard.analysis.cpa.util.StateNames;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;

/**
 * This run wraps the execution of BAM {@link JvmMemoryLocationCpa}.
 */
public class JvmTaintMemoryLocationBamCpaRun
    extends JvmMemoryLocationBamCpaRun<SimpleCpa, SetAbstractState<JvmTaintSource>>
{

    private final Collection<? extends JvmTaintSink>                                taintSinks;
    private       List<BamLocationDependentJvmMemoryLocation<?>>                    endPoints;
    private       Map<BamLocationDependentJvmMemoryLocation<?>, List<JvmTaintSink>> endPointToSinks;

    /**
     * Create a traced taint CPA run.
     *
     * @param jvmTaintCpaRun              an intraprocedural taint CPA run
     * @param threshold                   a cut-off threshold
     * @param taintSinks                  a collection of taint sinks
     * @param memoryLocationAbortOperator an abort operator for trace reconstruction
     */
    protected JvmTaintMemoryLocationBamCpaRun(JvmTaintBamCpaRun jvmTaintCpaRun,
                                              SetAbstractState<JvmTaintSource> threshold,
                                              Collection<? extends JvmTaintSink> taintSinks,
                                              AbortOperator memoryLocationAbortOperator)
    {
        super(jvmTaintCpaRun, threshold, memoryLocationAbortOperator);
        this.taintSinks = taintSinks;
    }

    /**
     * Create a traced taint CPA run.
     *
     * @param cfa                                      a CFA
     * @param taintSources                             a set of taint sources
     * @param mainSignature                            the main signature of the main method
     * @param maxCallStackDepth                        the maximum depth of the call stack analyzed interprocedurally.
     *                                                 0 means intraprocedural analysis.
     *                                                 < 0 means no maximum depth.
     * @param threshold                                a cut-off threshold
     * @param taintSinks                               a collection of taint sinks
     * @param abortOperator                            an abort operator
     * @param memoryLocationAbortOperator              an abort operator for trace reconstruction
     * @param reduceHeap                               whether reduction/expansion of the heap state is performed at call/return sites
     * @param principalHeapMapAbstractStateFactory     a map abstract state factory used for constructing the mapping from references to objects in the principal heap model
     * @param principalHeapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values in the principal heap model
     * @param followerHeapMapAbstractStateFactory      a map abstract state factory used for constructing the mapping from references to objects in the follower heap model
     * @param followerHeapNodeMapAbstractStateFactory  a map abstract state factory used for constructing the mapping from fields to values in the follower heap model
     */
    protected JvmTaintMemoryLocationBamCpaRun(JvmCfa cfa,
                                              Set<? extends JvmTaintSource> taintSources,
                                              MethodSignature mainSignature,
                                              int maxCallStackDepth,
                                              HeapModel heapModel,
                                              SetAbstractState<JvmTaintSource> threshold,
                                              Collection<? extends JvmTaintSink> taintSinks,
                                              AbortOperator abortOperator,
                                              AbortOperator memoryLocationAbortOperator,
                                              boolean reduceHeap,
                                              MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> staticFieldMapAbstractStateFactory,
                                              MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>> principalHeapMapAbstractStateFactory,
                                              MapAbstractStateFactory<String, SetAbstractState<Reference>> principalHeapNodeMapAbstractStateFactory,
                                              MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> followerHeapMapAbstractStateFactory,
                                              MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> followerHeapNodeMapAbstractStateFactory)
    {
        this(new JvmTaintBamCpaRun<JvmAbstractState<SetAbstractState<JvmTaintSource>>>(cfa,
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
                                                                                       followerHeapNodeMapAbstractStateFactory),
             threshold,
             taintSinks,
             memoryLocationAbortOperator);
    }

    public Collection<? extends JvmTaintSink> getTaintSinks()
    {
        return taintSinks;
    }

    public Map<BamLocationDependentJvmMemoryLocation<?>, List<JvmTaintSink>> getEndPointToSinks()
    {
        return endPointToSinks;
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

        Set<BamLocationDependentJvmMemoryLocation<?>> memoryLocations = new HashSet<>();
        Map<BamLocationDependentJvmMemoryLocation<?>, List<JvmTaintSink>> endPointToSinks = new HashMap<>();
        Map<Signature, Map<JvmTaintSink, Set<JvmMemoryLocation>>> fqnToSinkLocations = JvmTaintSink.convertSinksToMemoryLocations(taintSinks);

        // find reached taint sinks in all cached reached sets
        inputCpaRun.getCpa()
                   .getCache()
                   .values()
                   .stream()
                   .map(BlockAbstraction::getReachedSet)
                   .forEach(reachedSet -> reachedSet
                       .asCollection()
                       .forEach(state -> ((JvmAbstractState<SetAbstractState<JvmTaintSource>>) state.getStateByName(StateNames.Jvm))
                           .getProgramLocation()
                           .getLeavingEdges()
                           .forEach(edge -> createEndpointsForEdgeIfTainted(reachedSet, state, edge, fqnToSinkLocations, memoryLocations, endPointToSinks))));

        this.endPointToSinks = endPointToSinks;
        return endPoints = new ArrayList<>(memoryLocations);
    }

    /**
     * Creates a endpoint (entry point of the {@link JvmMemoryLocationCpa}) for each tainted location of a sink.
     *
     * @param reachedSet               A reached set containing the abstraction for one (or multiple if the entry states match) method calls
     * @param state                    A state that has to be checked to be a sink reached by a taint
     * @param edge                     A CFA edge that will be checked if it corresponds to a sink
     * @param signatureToSinkLocations A map from {@link Signature}s to corresponding {@link JvmTaintSink}s to all the locations that trigger the sink if tainted
     * @param memoryLocations          A set of endpoints. In case of tainted sink locations new states are added here
     * @param endPointToSinks          A mapping from the detected endpoints to corresponding sinks. In case of tainted sink locations new states are added here
     */
    private void createEndpointsForEdgeIfTainted(ReachedSet reachedSet,
                                                 AbstractState state,
                                                 JvmCfaEdge edge,
                                                 Map<Signature, Map<JvmTaintSink, Set<JvmMemoryLocation>>> signatureToSinkLocations,
                                                 Set<BamLocationDependentJvmMemoryLocation<?>> memoryLocations,
                                                 Map<BamLocationDependentJvmMemoryLocation<?>, List<JvmTaintSink>> endPointToSinks)
    {
        signatureToSinkLocations.getOrDefault(edge.targetSignature(), Collections.emptyMap())
                                .entrySet()
                                .stream()
                                .filter(e -> e.getKey().matchCfaEdge(edge))
                                .forEach(
                                    e -> e.getValue()
                                          .stream()
                                          .filter(l -> isStateTaintedForMemoryLocation((JvmAbstractState<SetAbstractState<JvmTaintSource>>) state.getStateByName(StateNames.Jvm), l, e.getKey()))
                                          .forEach(l -> createAndAddEndpoint(reachedSet, state, l, e.getKey(), memoryLocations, endPointToSinks)));
    }

    /**
     * Creates and adds an endpoint for each taint sink corresponding to a CFA edge triggered by a taint.
     *
     * @param reachedSet      A reached set containing the abstraction for one (or multiple if the entry states match) method calls
     * @param state           A state where a sink is reached by a taint
     * @param taintLocation   A sensitive location where the taint reaches the sink
     * @param sink            A sink reached by a taint
     * @param memoryLocations A set of endpoints. The new state is added here
     * @param endPointToSinks A mapping from the detected endpoints to corresponding sinks. The new state is added here
     */
    private void createAndAddEndpoint(ReachedSet reachedSet,
                                      AbstractState state,
                                      JvmMemoryLocation taintLocation,
                                      JvmTaintSink sink,
                                      Set<BamLocationDependentJvmMemoryLocation<?>> memoryLocations,
                                      Map<BamLocationDependentJvmMemoryLocation<?>, List<JvmTaintSink>> endPointToSinks)
    {
        BamLocationDependentJvmMemoryLocation<?> memoryLocation = new BamLocationDependentJvmMemoryLocation(taintLocation,
                                                                                                            ((ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>) state).getProgramLocation(),
                                                                                                            (ProgramLocationDependentReachedSet) reachedSet);
        memoryLocations.add(memoryLocation);
        endPointToSinks.computeIfAbsent(memoryLocation, x -> new ArrayList<>()).add(sink);
    }

    private boolean isStateTaintedForMemoryLocation(JvmAbstractState<SetAbstractState<JvmTaintSource>> state, JvmMemoryLocation memoryLocation, JvmTaintSink sink)
    {
        SetAbstractState<JvmTaintSource> extractedState = memoryLocation.extractValueOrDefault(state, SetAbstractState.bottom);
        extractedState.addAll(state.getHeap().getFieldOrDefault(memoryLocation, "", SetAbstractState.bottom));

        return extractedState.stream().anyMatch(sink.isValidForSource);
    }

    /**
     * A builder for {@link JvmTaintMemoryLocationBamCpaRun}. It assumes either the best performing parameters or the most basic one, if there is no absolute benefit.
     *
     * @author Dmitry Ivanov
     */
    public static class Builder
    {

        private JvmCfa                                                                         cfa;
        private MethodSignature                                                                mainSignature;
        private Set<? extends JvmTaintSource>                                                  taintSources                             = Collections.emptySet();
        private int                                                                            maxCallStackDepth                        = -1;
        private HeapModel                                                                      heapModel                                = HeapModel.FORGETFUL;
        private SetAbstractState<JvmTaintSource>                                               threshold                                = SetAbstractState.bottom;
        private Collection<? extends JvmTaintSink>                                             taintSinks                               = Collections.emptySet();
        private AbortOperator                                                                  abortOperator                            = NeverAbortOperator.INSTANCE;
        private AbortOperator                                                                  memoryLocationAbortOperator              = NeverAbortOperator.INSTANCE;
        private boolean                                                                        reduceHeap                               = true;
        private MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>              staticFieldMapAbstractStateFactory       = HashMapAbstractStateFactory.getInstance();
        private MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>>      principalHeapMapAbstractStateFactory     = HashMapAbstractStateFactory.getInstance();
        private MapAbstractStateFactory<String, SetAbstractState<Reference>>                   principalHeapNodeMapAbstractStateFactory = HashMapAbstractStateFactory.getInstance();
        private MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> followerHeapMapAbstractStateFactory      = HashMapAbstractStateFactory.getInstance();
        private MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>>              followerHeapNodeMapAbstractStateFactory  = HashMapAbstractStateFactory.getInstance();

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
                                                       memoryLocationAbortOperator,
                                                       reduceHeap,
                                                       staticFieldMapAbstractStateFactory,
                                                       principalHeapMapAbstractStateFactory,
                                                       principalHeapNodeMapAbstractStateFactory,
                                                       followerHeapMapAbstractStateFactory,
                                                       followerHeapNodeMapAbstractStateFactory);
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
        public Builder setThreshold(SetAbstractState<JvmTaintSource> threshold)
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
         * Sets the abort operator for premature trace reconstruction termination.
         */
        public Builder setMemoryLocationAbortOperator(AbortOperator memoryLocationAbortOperator)
        {
            this.memoryLocationAbortOperator = memoryLocationAbortOperator;
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
