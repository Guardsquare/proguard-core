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

package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.NeverAbortOperator;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.defaults.SimpleCpa;
import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.ProgramLocationDependentTransferRelation;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * A domain dependent analysis that can be wrapped with a {@link BamCpa} to be extended
 * inter-procedurally.
 *
 * <p>Compared to a standard {@link ConfigurableProgramAnalysis}, this is extended with three
 * operators:
 *
 * <p>- The reduce operator discards from the entry abstract state of a procedure the unnecessary
 * information (e.g. local variables of the caller).
 *
 * <p>- The expand operator restores the removed information.
 *
 * <p>- The rebuild operator avoids collision of program identifiers while returning from a
 * procedure call (e.g. renaming variables).
 *
 * @param <ContentT> The content of the jvm states. For example, this can be a {@link
 *     SetAbstractState} of taints for taint analysis or a {@link
 *     proguard.analysis.cpa.jvm.domain.value.ValueAbstractState} for value analysis.
 */
public class CpaWithBamOperators<ContentT extends LatticeAbstractState<ContentT>>
    extends SimpleCpa<JvmAbstractState<ContentT>> {

  private final ReduceOperator<ContentT> reduce;
  private final ExpandOperator<ContentT> expand;
  private final RebuildOperator rebuild;

  /**
   * Create a CPA with BAM operators from the abstract domain and the operators.
   *
   * @param abstractDomain a join-semilattice of {@link AbstractState}s defining the abstraction
   *     level of the analysis
   * @param transferRelation a transfer relation specifying how successor states are computed
   * @param merge a merge operator defining how (and whether) the older {@link AbstractState} should
   *     be updated with the newly discovered {@link AbstractState}
   * @param stop a stop operator deciding whether the successor state should be added to the {@link
   *     ReachedSet} based on the content of the latter
   * @param precisionAdjustment a precision adjustment selecting the {@link Precision} for the
   *     currently processed {@link AbstractState} considering the {@link ReachedSet} content
   * @param reduce a reduce operator discarding from the entry abstract state of procedures the
   *     unnecessary information
   * @param expand an expand operator restoring the information removed during reduction
   * @param rebuild a rebuild operator avoiding identifiers collision while returning from a
   *     procedure call
   */
  public CpaWithBamOperators(
      AbstractDomain<JvmAbstractState<ContentT>> abstractDomain,
      ProgramLocationDependentTransferRelation<ContentT> transferRelation,
      MergeOperator<JvmAbstractState<ContentT>> merge,
      StopOperator<JvmAbstractState<ContentT>> stop,
      PrecisionAdjustment precisionAdjustment,
      ReduceOperator<ContentT> reduce,
      ExpandOperator<ContentT> expand,
      RebuildOperator rebuild) {
    super(
        abstractDomain,
        transferRelation,
        merge,
        stop,
        precisionAdjustment,
        NeverAbortOperator.INSTANCE);
    this.reduce = reduce;
    this.expand = expand;
    this.rebuild = rebuild;
  }

  /**
   * Create a CPA with BAM operators from the intra-procedural {@link ConfigurableProgramAnalysis}
   * and the additional BAM operators.
   *
   * @param intraproceduralCpa an intra-procedural {@link ConfigurableProgramAnalysis}
   * @param reduce a reduce operator discarding from the entry abstract state of procedures the
   *     unnecessary information
   * @param expand an expand operator restoring the information removed during reduction
   * @param rebuild a rebuild operator avoiding identifiers collision while returning from a
   *     procedure call
   */
  public CpaWithBamOperators(
      ConfigurableProgramAnalysis<JvmAbstractState<ContentT>> intraproceduralCpa,
      ReduceOperator<ContentT> reduce,
      ExpandOperator<ContentT> expand,
      RebuildOperator rebuild) {
    super(
        intraproceduralCpa.getAbstractDomain(),
        intraproceduralCpa.getTransferRelation(),
        intraproceduralCpa.getMergeOperator(),
        intraproceduralCpa.getStopOperator(),
        intraproceduralCpa.getPrecisionAdjustment(),
        NeverAbortOperator.INSTANCE);
    this.reduce = reduce;
    this.expand = expand;
    this.rebuild = rebuild;
  }

  /** Returns the {@link ReduceOperator}. */
  public ReduceOperator<ContentT> getReduceOperator() {
    return reduce;
  }

  /** Returns the {@link ExpandOperator}. */
  public ExpandOperator<ContentT> getExpandOperator() {
    return expand;
  }

  /** Returns the {@link RebuildOperator}. */
  public RebuildOperator getRebuildOperator() {
    return rebuild;
  }
}
