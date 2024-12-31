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
 * The {@link AbstractDomain} defines a semilattice over {@link AbstractState}s.
 *
 * @param <StateT> The type of the analyzed states.
 */
public interface AbstractDomain<StateT extends AbstractState> {

  /**
   * Computes the join over two abstract states. To guarantee the correct behavior of the algorithm
   * implementations must have no side effects.
   */
  StateT join(StateT abstractState1, StateT abstractState2);

  /** Compares two abstract states. */
  boolean isLessOrEqual(StateT abstractState1, StateT abstractState2);
}
