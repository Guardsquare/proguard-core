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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An interface for {@link TransferRelation}s that depend on the {@link proguard.analysis.cpa.defaults.Cfa} location for which the successor can be defined for the edges of the current location.
 *
 * @author Carlo Alberto Pozzoli
 */
public interface ProgramLocationDependentTransferRelation<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    extends TransferRelation
{

    /**
     * Computes a successor state for the CFA {@code edge}.
     */
    AbstractState getEdgeAbstractSuccessor(AbstractState abstractState, CfaEdgeT edge, Precision precision);

    // implementations for TransferRelation

    @Override
    default Collection<? extends AbstractState> getAbstractSuccessors(AbstractState abstractState, Precision precision)
    {
        if (!(abstractState instanceof ProgramLocationDependent))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT> state = (ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT>) abstractState;
        Set<AbstractState> successors = new HashSet<>();
        for (CfaEdgeT edge : getEdges(state))
        {
            AbstractState successor = getEdgeAbstractSuccessor(abstractState, edge, precision);
            if (successor != null)
            {
                successors.add(successor);
            }
        }
        return successors;
    }

    List<CfaEdgeT> getEdges(ProgramLocationDependent<CfaNodeT, CfaEdgeT, SignatureT> state);
}
