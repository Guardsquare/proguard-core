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

import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.classfile.MethodSignature;

/**
 * The {@link JvmMemoryLocationCpa} backtraces memory locations. See {@see JvmMemoryLocationTransferRelation} for details.
 *
 * @param <AbstractStateT> The type of the values of the traced analysis.
 *
 * @author Dmitry Ivanov
 */
public class JvmMemoryLocationCpa<AbstractStateT extends LatticeAbstractState<AbstractStateT>>
    extends SimpleCpa
{

    public JvmMemoryLocationCpa(AbstractStateT threshold, BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa)
    {
        this(threshold, bamCpa, new DelegateAbstractDomain<JvmMemoryLocationAbstractState>());
    }

    private JvmMemoryLocationCpa(AbstractStateT threshold,
                                 BamCpa<JvmCfaNode, JvmCfaEdge, MethodSignature> bamCpa,
                                 AbstractDomain abstractDomain)
    {
        super(abstractDomain,
              new JvmMemoryLocationTransferRelation<>(threshold, bamCpa),
              new JvmMemoryLocationMergeJoinOperator(abstractDomain),
              new StopSepOperator(abstractDomain));
    }
}
