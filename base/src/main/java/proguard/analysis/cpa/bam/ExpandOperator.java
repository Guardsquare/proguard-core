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

import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.datastructure.callgraph.Call;

/**
 * This operator is used to recover the information discarded when entering a procedure block
 * depending on the domain-specific analysis.
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public interface ExpandOperator<ContentT extends AbstractState<ContentT>> {

  /**
   * Reconstructs the state of the caller of a procedure using the information of the expanded
   * initial state, the reduced exit state, the block entry node (that can be used to retrieve the
   * CFA subgraph of the function), and the call to the procedure.
   *
   * @param expandedInitialState the entry state of the called procedure before any reduction
   * @param reducedExitState the state of the called procedure in its exit node
   * @param blockEntryNode the entry node of the called procedure
   * @param call the information of the call to the procedure
   * @return The state of the caller after the procedure call, eventually with some collisions of
   *     identifiers that need the {@link RebuildOperator} to be solved
   */
  JvmAbstractState<ContentT> expand(
      JvmAbstractState<ContentT> expandedInitialState,
      JvmAbstractState<ContentT> reducedExitState,
      JvmCfaNode blockEntryNode,
      Call call);
}
