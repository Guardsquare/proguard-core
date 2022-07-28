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

import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;

/**
 * This operator is used to discard unnecessary information when entering a procedure block depending on the domain-specific analysis (e.g. local variables, caller stack).
 *
 * @author Carlo Alberto Pozzoli
 */
public interface ReduceOperator<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
{

    /**
     * Creates the initial state of the called procedure discarding the useless information from the state of the caller.
     *
     * @param expandedInitialState the entry state of the called procedure before any reduction
     * @param blockEntryNode       the entry node of the called procedure
     * @param call                 the information of the call to the procedure
     * @return The entry state of the called procedure
     */
    AbstractState reduce(AbstractState expandedInitialState, CfaNodeT blockEntryNode, Call call);
}
