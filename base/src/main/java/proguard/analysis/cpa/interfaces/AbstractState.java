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

/**
 * An {@link AbstractState} contains information about the program state.
 *
 * <p>Abstract states are meant as part of a join-semilattice representing the domain of the
 * analysis and implement operations over this set. In particular the states should be able to
 * determine whether they are below another state in the partial order and to calculate the least
 * upper bound on the semilattice when another state is provided (i.e., join operation).
 *
 * @param <StateT> recursive generic type of the abstract state.
 */
public interface AbstractState<StateT extends AbstractState<StateT>> {

  /** Returns the {@link Precision} used by the {@link PrecisionAdjustment}. */
  default Precision getPrecision() {
    return null;
  }

  /** Creates a copy of itself. */
  StateT copy();

  // overrides for Object

  @Override
  boolean equals(Object obj);

  @Override
  int hashCode();

  /**
   * Computes a join over itself and another abstract state {@code abstractState} (i.e., finds the
   * least upper bound on the semilattice).
   */
  StateT join(StateT abstractState);

  /**
   * Compares itself to the {@code abstractState} (i.e., compare the states on the partial order
   * provided by the domain of the analysis).
   */
  boolean isLessOrEqual(StateT abstractState);

  /** Strictly compares itself to the {@code abstractState}. */
  default boolean isLess(StateT abstractStateT) {
    return isLessOrEqual(abstractStateT) && !equals(abstractStateT);
  }
}
