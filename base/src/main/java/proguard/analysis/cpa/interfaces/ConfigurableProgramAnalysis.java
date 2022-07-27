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

package proguard.analysis.cpa.interfaces;

/**
 * {@link ConfigurableProgramAnalysis} consists of {@link AbstractDomain}, {@link TransferRelation}, {@link MergeOperator}, {@link StopOperator}, and {@link PrecisionAdjustment}.
 *
 * <p>The {@link AbstractDomain} is a join-semilattice of {@link AbstractState}s. It defines the abstraction level of the analysis.</p>
 *
 * <p>The {@link TransferRelation} specifies how successor states are computed in the {@link proguard.analysis.cpa.algorithms.CpaAlgorithm}.</p>
 *
 * <p>The {@link MergeOperator} defines how (and whether) the older {@link AbstractState} should be updated with the newly discovered {@link AbstractState}.</p>
 *
 * <p>The {@link StopOperator} decides whether the successor state should be added to the {@link ReachedSet} based on the content of the latter.</p>
 *
 * <p>The {@link PrecisionAdjustment} selects the {@link Precision} for the currently processed {@link AbstractState} considering the {@link ReachedSet} content.</p>
 *
 * <p>All CPA components should be side effect free, i.e., not modify their arguments.</p>
 *
 * @author Dmitry Ivanov
 */
public interface ConfigurableProgramAnalysis
{

    /**
     * Returns the abstract domain of this CPA.
     */
    AbstractDomain getAbstractDomain();

    /**
     * Returns the transfer relation of this CPA.
     */
    TransferRelation getTransferRelation();

    /**
     * Returns the merge operator of this CPA.
     */
    MergeOperator getMergeOperator();

    /**
     * Returns the stop operator of this CPA.
     */
    StopOperator getStopOperator();

    /**
     * Returns the precision adjustment of this CPA.
     */
    PrecisionAdjustment getPrecisionAdjustment();
}
