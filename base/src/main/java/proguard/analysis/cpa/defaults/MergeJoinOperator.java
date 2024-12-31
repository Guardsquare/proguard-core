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

import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * This {@link MergeOperator} applies the join operator to its arguments.
 *
 * @param <StateT> The type of the analyzed states.
 */
public final class MergeJoinOperator<StateT extends LatticeAbstractState<StateT>>
    implements MergeOperator<StateT> {
  private final AbstractDomain<StateT> abstractDomain;

  /**
   * Create a merge operator from an abstract domain defining the join operator.
   *
   * @param abstractDomain abstract domain
   */
  public MergeJoinOperator(AbstractDomain<StateT> abstractDomain) {
    this.abstractDomain = abstractDomain;
  }

  // implementations for MergeOperator

  @Override
  public StateT merge(StateT abstractState1, StateT abstractState2, Precision precision) {
    return abstractDomain.join(abstractState1, abstractState2);
  }
}
