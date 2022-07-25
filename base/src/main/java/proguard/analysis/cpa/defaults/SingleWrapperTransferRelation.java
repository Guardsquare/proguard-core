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

package proguard.analysis.cpa.defaults;

import java.util.Collection;
import java.util.Collections;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.interfaces.WrapperTransferRelation;

/**
 * This {@link WrapperTransferRelation} applies its (only) inner {@link TransferRelation}
 * to the input.
 *
 * @author Dmitry Ivanov
 */
public class SingleWrapperTransferRelation
    implements WrapperTransferRelation
{

    protected TransferRelation wrappedTransferRelation;

    /**
     * Create a wrapper transfer relation around the given {@link TransferRelation}.
     *
     * @param wrappedTransferRelation a transfer relation to be wrapped
     */
    public SingleWrapperTransferRelation(TransferRelation wrappedTransferRelation)
    {
        this.wrappedTransferRelation = wrappedTransferRelation;
    }

    // implementations for WrapperTransferRelation

    @Override
    public Iterable<TransferRelation> getWrappedTransferRelations()
    {
        return Collections.singletonList(wrappedTransferRelation);
    }

    // implementations for TransferRelation

    @Override
    public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState abstractState, Precision precision)
    {
        return wrappedTransferRelation.getAbstractSuccessors(abstractState, precision);
    }
}
