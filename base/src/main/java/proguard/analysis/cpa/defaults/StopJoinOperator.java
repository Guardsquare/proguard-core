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
import java.util.Iterator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.StopOperator;

/**
 * This {@link StopOperator} returns true if the input state is less or equal than join over the
 * reached set.
 *
 * @param <StateT> The type of the analyzed states.
 */
public final class StopJoinOperator<StateT extends AbstractState<StateT>>
    implements StopOperator<StateT> {

  // implementations for StopOperator

  @Override
  public boolean stop(
      StateT abstractState, Collection<StateT> reachedAbstractStates, Precision precision) {
    if (reachedAbstractStates.isEmpty()) {
      return false;
    }

    Iterator<StateT> reachedStatesIterator = reachedAbstractStates.iterator();
    StateT joinedReachedStates = reachedStatesIterator.next();
    while (reachedStatesIterator.hasNext()) {
      joinedReachedStates = joinedReachedStates.join(reachedStatesIterator.next());
    }

    return abstractState.isLessOrEqual(joinedReachedStates);
  }
}
