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

import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;

/**
 * The {@link JvmMemoryLocationCpa} backtraces memory locations. See {@see JvmMemoryLocationTransferRelation} for details.
 *
 * @author Dmitry Ivanov
 */
public class JvmMemoryLocationCpa<AbstractStateT extends LatticeAbstractState<AbstractStateT>>
    extends SimpleCpa
{

    /**
     * Create a memory location CPA.
     *
     * @param threshold  a cut-off threshold
     */
    public JvmMemoryLocationCpa(AbstractStateT threshold)
    {
        this(threshold, new DelegateAbstractDomain<JvmMemoryLocationAbstractState>());
    }

    private JvmMemoryLocationCpa(AbstractStateT threshold,
                                 AbstractDomain abstractDomain)
    {
        super(abstractDomain,
              new JvmMemoryLocationTransferRelation<>(threshold),
              new JvmMemoryLocationMergeJoinOperator(abstractDomain),
              new StopSepOperator(abstractDomain));
    }
}
