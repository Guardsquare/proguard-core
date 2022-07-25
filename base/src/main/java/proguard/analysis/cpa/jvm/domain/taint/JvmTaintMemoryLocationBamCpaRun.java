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
    public JvmTaintMemoryLocationBamCpaRun(JvmTaintBamCpaRun jvmTaintCpaRun,
                                           TaintAbstractState threshold,
                                           Collection<? extends JvmTaintSink> taintSinks)
    {
        super(jvmTaintCpaRun, threshold);
        this.taintSinks = taintSinks;
    }

    /**
     * Create a traced taint CPA run.
     *
     * @param cfa               a CFA
     * @param taintSources      a set of taint sources
     * @param mainSignature     the main signature of the main method
     * @param maxCallStackDepth the maximum depth of the call stack analyzed interprocedurally.
     *                          0 means intraprocedural analysis.
     *                          < 0 means no maximum depth.
     * @param threshold         a cut-off threshold
     * @param taintSinks        a collection of taint sinks
     * @param abortOperator     an abort operator
     */
    public JvmTaintMemoryLocationBamCpaRun(JvmCfa cfa,
                                           Set<? extends TaintSource> taintSources,
                                           MethodSignature mainSignature,
                                           int maxCallStackDepth,
                                           HeapModel heapModel,
                                           TaintAbstractState threshold,
                                           Collection<? extends JvmTaintSink> taintSinks,
                                           AbortOperator abortOperator)
    {
        this(new JvmTaintBamCpaRun(cfa, taintSources, mainSignature, maxCallStackDepth, heapModel, abortOperator), threshold, taintSinks);
    }

    /**
     * Create a traced taint CPA run without premature termination.
     *
     * @param cfa               a CFA
     * @param taintSources      a set of taint sources
     * @param mainSignature     the main signature of the main method
     * @param maxCallStackDepth the maximum depth of the call stack analyzed interprocedurally.
     *                          0 means intraprocedural analysis.
     *                          < 0 means no maximum depth.
     * @param threshold         a cut-off threshold
     * @param taintSinks        a collection of taint sinks
     */
    public JvmTaintMemoryLocationBamCpaRun(JvmCfa cfa,
                                           Set<? extends TaintSource> taintSources,
                                           MethodSignature mainSignature,
                                           int maxCallStackDepth,
                                           HeapModel heapModel,
                                           TaintAbstractState threshold,
                                           Collection<? extends JvmTaintSink> taintSinks)
    {
        this(new JvmTaintBamCpaRun(cfa, taintSources, mainSignature, maxCallStackDepth, heapModel, NeverAbortOperator.INSTANCE), threshold, taintSinks);
    }

    /**
     * Create a traced taint CPA run with a simple heap and no premature termination.
     *
     * @param cfa               a CFA
     * @param taintSources      a set of taint sources
     * @param mainSignature     the main signature of the main method
     * @param maxCallStackDepth maximum depth of the call stack analyzed inter-procedurally.
     *                          0 means intra-procedural analysis.
     *                          < 0 means no maximum depth.
     * @param threshold         a cut-off threshold
     * @param taintSinks        a collection of taint sinks
     */
    public JvmTaintMemoryLocationBamCpaRun(JvmCfa cfa,
                                           Set<? extends TaintSource> taintSources,
                                           MethodSignature mainSignature,
                                           int maxCallStackDepth,
                                           TaintAbstractState threshold,
                                           Collection<? extends JvmTaintSink> taintSinks)
    {
        this(new JvmTaintBamCpaRun(cfa, taintSources, mainSignature, maxCallStackDepth), threshold, taintSinks);
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
}
