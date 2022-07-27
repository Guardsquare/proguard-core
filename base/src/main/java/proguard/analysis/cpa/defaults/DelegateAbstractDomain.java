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

import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;

/**
 * This delegator passes all the {@link AbstractDomain} operators to the {@link LatticeAbstractState}.
 * Thus, we can keep the CPA algorithm generic without having to cast types in the domain specific code.
 * Abstract domains defined with {@link LatticeAbstractState} should be passed as {@link DelegateAbstractDomain}s
 * to the CPA algorithm.
 *
 * @author Dmitry Ivanov
 */
public class DelegateAbstractDomain<LatticeAbstractStateT extends LatticeAbstractState>
    implements AbstractDomain
{

    //implementations for AbstractDomain

    @Override
    public AbstractState join(AbstractState abstractState1, AbstractState abstractState2)
    {
        return ((LatticeAbstractStateT) abstractState1).join((LatticeAbstractStateT) abstractState2);
    }

    @Override
    public boolean isLessOrEqual(AbstractState abstractState1, AbstractState abstractState2)
    {
        return ((LatticeAbstractStateT) abstractState1).isLessOrEqual((LatticeAbstractStateT) abstractState2);
    }
}
