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

package proguard.analysis.cpa.domain.arg;

import java.util.Collections;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.classfile.Signature;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.ProgramLocationDependentTransferRelation;
import proguard.analysis.cpa.interfaces.TransferRelation;

/**
 * This {@link ArgTransferRelation} is extended for the case of {@link ProgramLocationDependent}
 * {@link AbstractState}s.
 *
 * @author Dmitry Ivanov
 */
public class ArgProgramLocationDependentTransferRelation<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    extends ArgTransferRelation
    implements ProgramLocationDependentTransferRelation<CfaNodeT, CfaEdgeT, SignatureT>
{

    /**
     * Create a wrapper transfer relation.
     *
     * @param wrappedTransferRelation a transfer relation to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     */
    public ArgProgramLocationDependentTransferRelation(TransferRelation wrappedTransferRelation, ArgAbstractStateFactory argAbstractStateFactory)
    {
        super(wrappedTransferRelation, argAbstractStateFactory);
    }

    // implementations for ProgramLocationDependentTransferRelation

    @Override
    public AbstractState getEdgeAbstractSuccessor(AbstractState abstractState, CfaEdge edge, Precision precision)
    {
        if (!(abstractState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        ArgAbstractState predecessorState = (ArgAbstractState) abstractState;
        return argAbstractStateFactory
            .createArgAbstractState(((ProgramLocationDependentTransferRelation) wrappedTransferRelation).getEdgeAbstractSuccessor(predecessorState.getWrappedState(),
                                                                                                                                  edge,
                                                                                                                                  precision),
                                    Collections.singletonList(predecessorState));
    }
}
