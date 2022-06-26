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

package proguard.analysis.cpa.jvm.domain.memory;

import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * This {@link MergeOperator} applies the join operator to its arguments sharing the same memory location.
 *
 * @author Dmitry Ivanov
 */
public final class JvmMemoryLocationMergeJoinOperator
    implements MergeOperator
{
    private final AbstractDomain abstractDomain;

    /**
     * Create a merge operator from an abstract domain defining the join operator.
     *
     * @param abstractDomain abstract domain
     */
    public JvmMemoryLocationMergeJoinOperator(AbstractDomain abstractDomain)
    {
        this.abstractDomain = abstractDomain;
    }

    // implementations for MergeOperator

    @Override
    public AbstractState merge(AbstractState abstractState1, AbstractState abstractState2, Precision precision)
    {
        if (!((JvmMemoryLocationAbstractState) abstractState1).getLocationDependentMemoryLocation().equals(((JvmMemoryLocationAbstractState) abstractState2).getLocationDependentMemoryLocation()))
        {
            return abstractState2;
        }
        return abstractDomain.join(abstractState1, abstractState2);
    }
}
