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
 * @author Dmitry Ivanov
 */
public class SimpleCpa
    implements ConfigurableProgramAnalysis
{
    private final AbstractDomain      abstractDomain;
    private final TransferRelation    transferRelation;
    private final MergeOperator       mergeOperator;
    private final StopOperator        stopOperator;
    private final PrecisionAdjustment precisionAdjustment;

    /**
     * Create a simple CPA with a static precision adjustment.
     *
     * @param abstractDomain   a join-semilattice of {@link AbstractState}s defining the abstraction level of the analysis
     * @param transferRelation a transfer relation specifying how successor states are computed
     * @param mergeOperator    a merge operator defining how (and whether) the older {@link AbstractState} should be updated
     *                         with the newly discovered {@link AbstractState}
     * @param stopOperator     a stop operator deciding whether the successor state should be added to the {@link ReachedSet} based on the content of the latter
     */
    public SimpleCpa(AbstractDomain abstractDomain, TransferRelation transferRelation, MergeOperator mergeOperator, StopOperator stopOperator)
    {
        this(abstractDomain, transferRelation, mergeOperator, stopOperator, new StaticPrecisionAdjustment());
    }

    /**
     * Create a simple CPA from {@link ConfigurableProgramAnalysis} components.
     *
     * @param abstractDomain      a join-semilattice of {@link AbstractState}s defining the abstraction level of the analysis
     * @param transferRelation    a transfer relation specifying how successor states are computed
     * @param mergeOperator       a merge operator defining how (and whether) the older {@link AbstractState} should be updated
     *                            with the newly discovered {@link AbstractState}
     * @param stopOperator        a stop operator deciding whether the successor state should be added to the {@link ReachedSet} based on the content of the latter
     * @param precisionAdjustment a precision adjustment selecting the {@link Precision} for the currently processed {@link AbstractState}
     *                            considering the {@link ReachedSet} content
     */
    public SimpleCpa(AbstractDomain abstractDomain, TransferRelation transferRelation, MergeOperator mergeOperator, StopOperator stopOperator, PrecisionAdjustment precisionAdjustment)
    {
        this.abstractDomain = abstractDomain;
        this.transferRelation = transferRelation;
        this.mergeOperator = mergeOperator;
        this.stopOperator = stopOperator;
        this.precisionAdjustment = precisionAdjustment;
    }

    // implementations for ConfigurableProgramAnalysis

    @Override
    public AbstractDomain getAbstractDomain()
    {
        return abstractDomain;
    }

    @Override
    public TransferRelation getTransferRelation()
    {
        return transferRelation;
    }

    @Override
    public MergeOperator getMergeOperator()
    {
        return mergeOperator;
    }

    @Override
    public StopOperator getStopOperator()
    {
        return stopOperator;
    }

    @Override
    public PrecisionAdjustment getPrecisionAdjustment()
    {
        return precisionAdjustment;
    }
}
