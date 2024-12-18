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
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.ProgramLocationDependentReachedSet;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.classfile.MethodSignature;

/**
 * This interface contains helper methods for producing witness traces.
 *
 * @param <T> The type of the states contained in the JVM state. e.g., for taint analysis this would
 *     be a {@link proguard.analysis.cpa.defaults.SetAbstractState} containing the taints and for
 *     value analysis a {@link proguard.analysis.cpa.jvm.domain.value.ValueAbstractState}.
 */
public interface TraceExtractor<T extends LatticeAbstractState<T>> {

  /** Returns a set of linear witness traces. */
  default Set<List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>>>
      extractLinearTraces() {
    Set<List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>>> result = new HashSet<>();
    for (BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>> endpoint : getEndPoints()) {
      List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> trace = new ArrayList<>();
      trace.add(endpoint);
      traceExtractionIteration(result, trace);
    }
    return result.stream().map(this::removeDuplicateProgramLocations).collect(Collectors.toSet());
  }

  /**
   * Returns endpoints or the extracted traces. Its output should be used for constructing initial
   * states for memory location CPAs.
   */
  Collection<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> getEndPoints();

  /** Returns the reached set of a trace extracting memory location CPA. */
  ProgramLocationDependentReachedSet<
          JvmCfaNode,
          JvmCfaEdge,
          JvmMemoryLocationAbstractState<JvmAbstractState<T>>,
          MethodSignature>
      getTraceReconstructionReachedSet();

  default void traceExtractionIteration(
      Set<List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>>> result,
      List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> currentTrace) {
    BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>> currentNode =
        currentTrace.get(currentTrace.size() - 1);
    List<JvmMemoryLocationAbstractState<JvmAbstractState<T>>> currentStates = new ArrayList<>();
    for (JvmMemoryLocationAbstractState<JvmAbstractState<T>> state :
        getTraceReconstructionReachedSet().getReached(currentNode.getProgramLocation())) {
      if (state.getLocationDependentMemoryLocation().equals(currentNode)) {
        currentStates.add(state);
      }
    }

    for (JvmMemoryLocationAbstractState<JvmAbstractState<T>> currentState : currentStates) {
      Set<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> sourceLocations =
          currentState.getSourceLocations();

      if (sourceLocations.isEmpty()) {
        result.add(currentTrace);
      }
      for (BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>> location : sourceLocations) {
        if (currentTrace.contains(location)) {
          continue;
        }
        List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> trace =
            new ArrayList<>(currentTrace);
        trace.add(location);
        traceExtractionIteration(result, trace);
      }
    }
  }

  default List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>>
      removeDuplicateProgramLocations(
          List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> trace) {
    List<BamLocationDependentJvmMemoryLocation<JvmAbstractState<T>>> result = new ArrayList<>();
    result.add(trace.get(0));
    for (int i = 1; i < trace.size(); i++) {
      // remove irrelevant nodes
      if (trace.get(i).getProgramLocation().getOffset() >= 0 // remove method exits
          && (result.get(result.size() - 1).getProgramLocation().getOffset()
                  != trace
                      .get(i)
                      .getProgramLocation()
                      .getOffset() // remove nodes related to the same program point
              // (signature:offset)
              || !Objects.equals(
                  result.get(result.size() - 1).getProgramLocation().getSignature(), // repeatedly
                  trace.get(i).getProgramLocation().getSignature()))) {
        result.add(trace.get(i));
      }
    }
    return result;
  }
}
