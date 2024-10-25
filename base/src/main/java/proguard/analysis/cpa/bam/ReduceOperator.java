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

import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;

/**
 * This operator is used to discard unnecessary information when entering a procedure block
 * depending on the domain-specific analysis (e.g. local variables, caller stack).
 */
public interface ReduceOperator<
    CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>,
    CfaEdgeT extends CfaEdge<CfaNodeT>,
    SignatureT extends Signature> {

  /**
   * Accumulates the reduction procedure by calling the method to create the initial state of the
   * called procedure discarding the useless information from the state of the caller and calling a
   * method that performs additional operations on the created state if any are specified by an
   * implementing class.
   *
   * @param expandedInitialState the entry state of the called procedure before any reduction
   * @param blockEntryNode the entry node of the called procedure
   * @param call the information of the call to the procedure
   * @return The entry state of the called procedure
   */
  default AbstractState reduce(
      AbstractState expandedInitialState, CfaNodeT blockEntryNode, Call call) {
    AbstractState state = reduceImpl(expandedInitialState, blockEntryNode, call);
    return onMethodEntry(state, call.isStatic());
  }

  /**
   * Creates the initial state of the called procedure discarding the useless information from the
   * state of the caller.
   *
   * @param expandedInitialState the entry state of the called procedure before any reduction
   * @param blockEntryNode the entry node of the called procedure
   * @param call the information of the call to the procedure
   * @return The entry state of the called procedure
   */
  AbstractState reduceImpl(AbstractState expandedInitialState, CfaNodeT blockEntryNode, Call call);

  /**
   * Performs additional operations on the reduced state (i.e. on the method entry state). Does
   * nothing by default. NB: since this is still part of the reduction operation the result of this
   * method is the state used by the analysis and part of the cache key for the called method.
   *
   * @param reducedState reduced state (i.e., the entry state of the called method)
   * @param isStatic is the called method static
   * @return the state after performing additional operations or untouched state by default
   */
  default AbstractState onMethodEntry(AbstractState reducedState, boolean isStatic) {
    return reducedState;
  }
}
