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

import proguard.classfile.Signature;

import java.util.List;

/**
 * An interface for {@link TransferRelation}s that depend on the {@link proguard.analysis.cpa.defaults.Cfa} location for which the successor can be defined for the entering edges of the current location.
 *
 * @author James Hamilton
 */

public interface ProgramLocationDependentBackwardTransferRelation<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
        extends ProgramLocationDependentTransferRelation<CfaNodeT, CfaEdgeT, SignatureT>
{
    @Override
    default List<CfaEdgeT> getEdges(ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT> state) {
        return state.getProgramLocation().getEnteringEdges();
    }
}
