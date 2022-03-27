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
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;

/**
 * This {@link ReduceOperator} returns the original {@link AbstractState} without performing any reduction.
 *
 * @author Carlo Alberto Pozzoli
 */
public class NoOpReduceOperator<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    implements ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT>
{

    // Implementations for ReduceOperator

    @Override
    public AbstractState reduce(AbstractState expandedInitialState, CfaNodeT blockEntryNode, Call call)
    {
        AbstractState result = expandedInitialState.copy();
        ((ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) result).setProgramLocation(blockEntryNode);
        return result;
    }
}
