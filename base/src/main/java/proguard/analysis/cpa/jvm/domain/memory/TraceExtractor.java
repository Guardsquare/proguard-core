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

package proguard.analysis.cpa.jvm.domain.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.CpaRun;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;

/**
 * This interfaces containts helper methods for {@link CpaRun}s
 * producing witness traces.
 *
 * @author Dmitry Ivanov
 */
public interface TraceExtractor
{

    /**
     * Returns a set of linear witness traces.
     */
    default Set<List<BamLocationDependentJvmMemoryLocation>> extractLinearTraces()
    {
        Set<List<BamLocationDependentJvmMemoryLocation>> result = new HashSet<>();
        for (BamLocationDependentJvmMemoryLocation<?> l : getEndPoints())
        {
            List<BamLocationDependentJvmMemoryLocation> trace = new ArrayList<>();
            trace.add(l);
            traceExtractionIteration(result, trace);
        }
        return result.stream().map(this::removeDuplicateProgramLocations).collect(Collectors.toSet());
    }

    /**
     * Returns endpoints or the extracted traces. Its output should be used for constructing initial states for memory location CPAs.
     */
    Collection<BamLocationDependentJvmMemoryLocation<?>> getEndPoints();

    /**
     * Returns the reached set of a trace extracting memory location CPA.
     */
    ProgramLocationDependentReachedSet<JvmCfaNode, JvmCfaEdge, JvmMemoryLocationAbstractState<?>, MethodSignature> getOutputReachedSet();

    default void traceExtractionIteration(Set<List<BamLocationDependentJvmMemoryLocation>> result,
                                          List<BamLocationDependentJvmMemoryLocation> currentTrace)
    {
        BamLocationDependentJvmMemoryLocation currentNode = currentTrace.get(currentTrace.size() - 1);
        List<JvmMemoryLocationAbstractState> currentStates = new ArrayList<>();
        for (AbstractState s : getOutputReachedSet().getReached(currentNode.getProgramLocation()))
        {
            if (((JvmMemoryLocationAbstractState) s).getLocationDependentMemoryLocation().equals(currentNode))
            {
                currentStates.add((JvmMemoryLocationAbstractState) s);
            }
        }

        for (JvmMemoryLocationAbstractState currentState : currentStates)
        {
            Set<BamLocationDependentJvmMemoryLocation> sourceLocations = currentState.getSourceLocations();

            if (sourceLocations.size() == 0)
            {
                result.add(currentTrace);
            }
            for (BamLocationDependentJvmMemoryLocation l : sourceLocations)
            {
                if (currentTrace.contains(l))
                {
                    continue;
                }
                List<BamLocationDependentJvmMemoryLocation> trace = new ArrayList<>(currentTrace);
                trace.add(l);
                traceExtractionIteration(result, trace);
            }
        }
    }

    default List<BamLocationDependentJvmMemoryLocation> removeDuplicateProgramLocations(List<BamLocationDependentJvmMemoryLocation> trace)
    {
        List<BamLocationDependentJvmMemoryLocation> result = new ArrayList<>();
        result.add(trace.get(0));
        for (int i = 1; i < trace.size(); i++)
        {
            // remove irrelevant nodes
            if (trace.get(i).getProgramLocation().getOffset() >= 0 // remove method exits
                && (result.get(result.size() - 1).getProgramLocation().getOffset() != trace.get(i).getProgramLocation().getOffset() // remove nodes related to the same program point (signature:offset)
                    || !Objects.equals(result.get(result.size() - 1).getProgramLocation().getSignature(),                           // repeatedly
                                       trace.get(i).getProgramLocation().getSignature())))
            {
                result.add(trace.get(i));
            }
        }
        return result;
    }
}
