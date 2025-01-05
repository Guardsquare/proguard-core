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

import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * This {@link MergeOperator} applies the join operator to its arguments sharing the same memory
 * location.
 *
 * @param <ContentT> The content of the jvm states for the traced analysis. For example, this can be
 *     a {@link SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public final class JvmMemoryLocationMergeJoinOperator<ContentT extends AbstractState<ContentT>>
    implements MergeOperator<JvmMemoryLocationAbstractState<ContentT>> {

  // implementations for MergeOperator

  @Override
  public JvmMemoryLocationAbstractState<ContentT> merge(
      JvmMemoryLocationAbstractState<ContentT> abstractState1,
      JvmMemoryLocationAbstractState<ContentT> abstractState2,
      Precision precision) {
    if (!abstractState1
        .getLocationDependentMemoryLocation()
        .equals(abstractState2.getLocationDependentMemoryLocation())) {
      return abstractState2;
    }
    return abstractState1.join(abstractState2);
  }
}
