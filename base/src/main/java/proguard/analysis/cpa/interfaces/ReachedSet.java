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

package proguard.analysis.cpa.interfaces;

import java.util.Collection;

/**
 * The {@link ReachedSet} stores reached {@code State}s discovered by the {@link
 * proguard.analysis.cpa.algorithms.CpaAlgorithm}.
 *
 * @param <StateT> The states contained in the reached set.
 */
public interface ReachedSet<StateT extends AbstractState<StateT>> {

  /** Adds an abstract state. */
  boolean add(StateT abstractState);

  /** Adds multiple abstract states. */
  boolean addAll(Collection<? extends StateT> abstractStates);

  /** Removes an abstract state. */
  boolean remove(StateT abstractState);

  /** Removes multiple abstract states. */
  boolean removeAll(Collection<? extends StateT> abstractStates);

  /** Returns a collection representation of itself. */
  Collection<StateT> asCollection();

  /** Returns a collection of abstract states mergeable with the {@code abstractState}. */
  Collection<StateT> getReached(StateT abstractState);

  /** Empties the reached set. */
  void clear();
}
