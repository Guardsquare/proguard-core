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
import java.util.HashSet;
import java.util.LinkedHashSet;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ReachedSet;

/**
 * This is a {@link LinkedHashSet}-based implementation of the {@link ReachedSet}.
 *
 * @param <StateT> The states contained in the reached set.
 */
public final class DefaultReachedSet<StateT extends AbstractState> implements ReachedSet<StateT> {

  HashSet<StateT> reachedSet = new LinkedHashSet<>();

  // implementations for ReachedSet

  @Override
  public boolean add(StateT abstractState) {
    return reachedSet.add(abstractState);
  }

  @Override
  public boolean addAll(Collection<? extends StateT> abstractStates) {
    return reachedSet.addAll(abstractStates);
  }

  @Override
  public boolean remove(StateT state) {
    return reachedSet.remove(state);
  }

  @Override
  public boolean removeAll(Collection<? extends StateT> abstractStates) {
    return reachedSet.removeAll(abstractStates);
  }

  @Override
  public Collection<StateT> asCollection() {
    return Collections.unmodifiableSet(reachedSet);
  }

  @Override
  public Collection<StateT> getReached(StateT abstractState) {
    return asCollection();
  }

  @Override
  public void clear() {
    reachedSet.clear();
  }
}
