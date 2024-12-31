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

import java.util.Map;
import java.util.Set;
import proguard.analysis.cpa.bam.BamCpa;
import proguard.analysis.cpa.defaults.DelegateAbstractDomain;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.defaults.StopSepOperator;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.datastructure.callgraph.Call;

/**
 * The {@link JvmMemoryLocationCpa} backtraces memory locations. See {@see
 * JvmMemoryLocationTransferRelation} for details.
 *
 * @param <ContentT> The content of the jvm states for the traced analysis. For example, this can be
 *     a {@link SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class JvmMemoryLocationCpa<ContentT extends LatticeAbstractState<ContentT>>
    extends SimpleCpa<JvmMemoryLocationAbstractState<ContentT>> {

  private final AbortOperator abortOperator;

  public JvmMemoryLocationCpa(
      ContentT threshold,
      BamCpa<ContentT> bamCpa,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations,
      AbortOperator abortOperator) {
    this(
        threshold,
        bamCpa,
        new DelegateAbstractDomain<JvmMemoryLocationAbstractState<ContentT>>(),
        extraTaintPropagationLocations,
        abortOperator);
  }

  private JvmMemoryLocationCpa(
      ContentT threshold,
      BamCpa<ContentT> bamCpa,
      AbstractDomain<JvmMemoryLocationAbstractState<ContentT>> abstractDomain,
      Map<Call, Set<JvmMemoryLocation>> extraTaintPropagationLocations,
      AbortOperator abortOperator) {
    super(
        abstractDomain,
        new JvmMemoryLocationTransferRelation<>(threshold, bamCpa, extraTaintPropagationLocations),
        new JvmMemoryLocationMergeJoinOperator(abstractDomain),
        new StopSepOperator(abstractDomain));
    this.abortOperator = abortOperator;
  }

  public AbortOperator getAbortOperator() {
    return abortOperator;
  }
}
