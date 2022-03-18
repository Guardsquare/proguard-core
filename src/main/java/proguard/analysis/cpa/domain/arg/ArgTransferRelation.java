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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.SingleWrapperTransferRelation;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.TransferRelation;

/**
 * This {@link SingleWrapperTransferRelation} applies the wrapped {@link TransferRelation}
 * to the {@link AbstractState} stored in an ARG node and wraps its output into a child
 * ARG node.
 *
 * @author Dmitry Ivanov
 */
public class ArgTransferRelation
    extends SingleWrapperTransferRelation
{

    protected final ArgAbstractStateFactory argAbstractStateFactory;

    /**
     * Create a wrapper transfer relation.
     *
     * @param wrappedTransferRelation a transfer relation to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     */
    public ArgTransferRelation(TransferRelation wrappedTransferRelation, ArgAbstractStateFactory argAbstractStateFactory)
    {
        super(wrappedTransferRelation);
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for TransferRelation

    @Override
    public Collection<? extends ArgAbstractState> getAbstractSuccessors(AbstractState abstractState, Precision precision)
    {
        if (!(abstractState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        ArgAbstractState predecessorState = (ArgAbstractState) abstractState;
        return wrappedTransferRelation.getAbstractSuccessors(predecessorState.getWrappedState(),
                                                             precision)
                                      .stream()
                                      .map(s -> argAbstractStateFactory.createArgAbstractState(s,
                                                                                               Collections.singletonList(predecessorState)))
                                      .collect(Collectors.toSet());
    }
}
