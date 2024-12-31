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

package proguard.analysis.cpa.defaults;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * This {@link ReachedSet} stores {@link ProgramLocationDependent} {@link AbstractState}s. It
 * assumes the analysis does merge the {@link AbstractState}s belonging to different {@link
 * CfaNode}s and stores them in separate bins.
 *
 * @param <StateT> The type of the {@link ProgramLocationDependent} abstract states contained in the
 *     reached set. Typically, a {@link JvmAbstractState}, but might be a different type of state
 *     depending on the analysis (e.g., might contain {@link
 *     proguard.analysis.cpa.jvm.domain.memory.JvmMemoryLocationAbstractState} for taint trace
 *     analysis).
 */
public final class ProgramLocationDependentReachedSet<
        StateT extends AbstractState & ProgramLocationDependent>
    implements ReachedSet<StateT> {

  private final Map<JvmCfaNode, Set<StateT>> locationToStates = new LinkedHashMap<>();

  // implementations for ReachedSet

  @Override
  public boolean add(StateT abstractState) {
    return locationToStates
        .computeIfAbsent(abstractState.getProgramLocation(), x -> new LinkedHashSet<>())
        .add(abstractState);
  }

  @Override
  public boolean addAll(Collection<? extends StateT> abstractStates) {
    boolean result = false;

    for (StateT state : abstractStates) {
      result |= add(state);
    }

    return result;
  }

  @Override
  public boolean remove(StateT abstractState) {
    JvmCfaNode location = abstractState.getProgramLocation();
    return locationToStates.containsKey(location)
        && locationToStates.get(location).remove(abstractState);
  }

  @Override
  public boolean removeAll(Collection<? extends StateT> abstractStates) {
    boolean result = false;

    for (StateT state : abstractStates) {
      result |= remove(state);
    }

    return result;
  }

  @Override
  public Collection<StateT> asCollection() {
    int initialSize = locationToStates.values().size();
    return locationToStates.values().stream()
        .reduce(
            new LinkedHashSet<>(initialSize),
            (x, y) -> {
              x.addAll(y);
              return x;
            });
  }

  @Override
  public Collection<StateT> getReached(StateT abstractState) {
    return getReached((abstractState.getProgramLocation()));
  }

  /** Returns a collection of abstract states belonging to the given {@code location}. */
  public Collection<StateT> getReached(JvmCfaNode location) {
    return locationToStates.getOrDefault(location, Collections.emptySet());
  }

  @Override
  public void clear() {
    locationToStates.clear();
  }
}
