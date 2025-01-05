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

import org.jetbrains.annotations.NotNull;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.TransferRelation;

/**
 * The {@link SimpleCpa} is a {@link ConfigurableProgramAnalysis} wrapping its components.
 *
 * @param <StateT> The type of the analyzed states.
 */
public class SimpleCpa<StateT extends AbstractState<StateT>>
    implements ConfigurableProgramAnalysis<StateT> {
  private final TransferRelation<StateT> transferRelation;
  private final MergeOperator<StateT> mergeOperator;
  private final StopOperator<StateT> stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final AbortOperator abortOperator;

  /**
   * Create a simple CPA with a static precision adjustment.
   *
   * @param abstractDomain a join-semilattice of {@link AbstractState}s defining the abstraction
   *     level of the analysis
   * @param transferRelation a transfer relation specifying how successor states are computed
   * @param mergeOperator a merge operator defining how (and whether) the older {@link
   *     AbstractState} should be updated with the newly discovered {@link AbstractState}
   * @param stopOperator a stop operator deciding whether the successor state should be added to the
   *     {@link ReachedSet} based on the content of the latter
   */
  public SimpleCpa(
      TransferRelation<StateT> transferRelation,
      MergeOperator<StateT> mergeOperator,
      StopOperator<StateT> stopOperator) {
    this(
        transferRelation,
        mergeOperator,
        stopOperator,
        new StaticPrecisionAdjustment(),
        NeverAbortOperator.INSTANCE);
  }

  /**
   * Create a simple CPA from {@link ConfigurableProgramAnalysis} components.
   *
   * @param abstractDomain a join-semilattice of {@link AbstractState}s defining the abstraction
   *     level of the analysis
   * @param transferRelation a transfer relation specifying how successor states are computed
   * @param mergeOperator a merge operator defining how (and whether) the older {@link
   *     AbstractState} should be updated with the newly discovered {@link AbstractState}
   * @param stopOperator a stop operator deciding whether the successor state should be added to the
   *     {@link ReachedSet} based on the content of the latter
   * @param precisionAdjustment a precision adjustment selecting the {@link Precision} for the
   *     currently processed {@link AbstractState} considering the {@link ReachedSet} content
   * @param abortOperator an operator used to terminate the analysis prematurely.
   */
  public SimpleCpa(
      TransferRelation<StateT> transferRelation,
      MergeOperator<StateT> mergeOperator,
      StopOperator<StateT> stopOperator,
      PrecisionAdjustment precisionAdjustment,
      AbortOperator abortOperator) {
    this.transferRelation = transferRelation;
    this.mergeOperator = mergeOperator;
    this.stopOperator = stopOperator;
    this.precisionAdjustment = precisionAdjustment;
    this.abortOperator = abortOperator;
  }

  // implementations for ConfigurableProgramAnalysis

  @Override
  public @NotNull TransferRelation<StateT> getTransferRelation() {
    return transferRelation;
  }

  @Override
  public @NotNull MergeOperator<StateT> getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public @NotNull StopOperator<StateT> getStopOperator() {
    return stopOperator;
  }

  @Override
  public @NotNull PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public @NotNull AbortOperator getAbortOperator() {
    return abortOperator;
  }
}
