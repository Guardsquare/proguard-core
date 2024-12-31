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

package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.datastructure.callgraph.Call;

/**
 * This {@link ReduceOperator} returns the original {@link AbstractState} without performing any
 * reduction.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class NoOpReduceOperator<ContentT extends LatticeAbstractState<ContentT>>
    implements ReduceOperator<ContentT> {

  // Implementations for ReduceOperator

  @Override
  public JvmAbstractState<ContentT> reduceImpl(
      JvmAbstractState<ContentT> expandedInitialState, JvmCfaNode blockEntryNode, Call call) {
    JvmAbstractState<ContentT> result = expandedInitialState.copy();
    result.setProgramLocation(blockEntryNode);
    return result;
  }
}
