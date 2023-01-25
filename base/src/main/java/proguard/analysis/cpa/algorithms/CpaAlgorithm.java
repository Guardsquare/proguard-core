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

package proguard.analysis.cpa.algorithms;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.analysis.cpa.defaults.PrecisionAdjustmentResult;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Algorithm;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.StopOperator;
import proguard.analysis.cpa.interfaces.TransferRelation;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This is the <a href="https://www.sosy-lab.org/research/pub/2018-JAR.A_Unifying_View_on_SMT-Based_Software_Verification.pdf">CPA+</a> {@link Algorithm}. The algorithm computes the set of reached
 * states based on the initial content of the waitlist.
 *
 * @author Dmitry Ivanov
 */
public class CpaAlgorithm
    implements Algorithm
{

    private static final Logger              log = LogManager.getLogger(CpaAlgorithm.class);
    private final        TransferRelation    transferRelation;
    private final        MergeOperator       mergeOperator;
    private final        StopOperator        stopOperator;
    private final        PrecisionAdjustment precisionAdjustment;

    /**
     * Create an algorithm to run the specified CPA.
     *
     * @param cpa a CPA instance wrapping the transfer relation, the merge, and the stop operator, and the precision adjustment
     */
    public CpaAlgorithm(ConfigurableProgramAnalysis cpa)
    {
        this(cpa.getTransferRelation(),
             cpa.getMergeOperator(),
             cpa.getStopOperator(),
             cpa.getPrecisionAdjustment());
    }

    /**
     * Create a CPA algorithm from CPA components.
     *
     * @param transferRelation    a transfer relation specifying how successor states are computed
     * @param mergeOperator       a merge operator defining how (and whether) the older {@link AbstractState} should be updated with the newly discovered {@link AbstractState}
     * @param stopOperator        a stop operator deciding whether the successor state should be added to the {@link ReachedSet} based on the content of the latter
     * @param precisionAdjustment a precision adjustment selecting the {@link Precision} for the currently processed {@link AbstractState} considering the {@link ReachedSet} content
     */
    public CpaAlgorithm(TransferRelation transferRelation, MergeOperator mergeOperator, StopOperator stopOperator, PrecisionAdjustment precisionAdjustment)
    {
        this.transferRelation = transferRelation;
        this.mergeOperator = mergeOperator;
        this.stopOperator = stopOperator;
        this.precisionAdjustment = precisionAdjustment;
    }

    /**
     * Algorithm from the paper is parametrized with the reached set and the waitlist. Thus one can select the start point of the algorithm (e.g., for resuming the analysis).
     * The {@code abortOperator} determines whether the analysis should end prematurely.
     */
    @Override
    public void run(ReachedSet reachedSet, Waitlist waitlist, AbortOperator abortOperator)
    {
        while (!waitlist.isEmpty())
        {
            AbstractState currentState = waitlist.pop();
            try
            {
                if (abortOperator.abort(currentState))
                {
                    return;
                }
                Precision                 currentPrecision          = currentState.getPrecision();
                PrecisionAdjustmentResult precisionAdjustmentResult = precisionAdjustment.prec(currentState, currentPrecision, reachedSet.getReached(currentState));
                currentState = precisionAdjustmentResult.getAbstractState();
                currentPrecision = currentState.getPrecision();

                for (AbstractState successorState : transferRelation.getAbstractSuccessors(currentState, currentPrecision))
                {
                    Set<AbstractState> gen  = new HashSet<>(); // abstract states to be added to the waitlist and reached set
                    Set<AbstractState> kill = new HashSet<>(); // abstract states to be removed from the waitlist and reached set
                    for (AbstractState reachedState : reachedSet.getReached(successorState)) // iterate only over the reached sets which may be merged with the successor state
                    {
                        AbstractState mergedState = mergeOperator.merge(successorState, reachedState, successorState.getPrecision());
                        if (!mergedState.equals(reachedState))
                        {
                            gen.add(mergedState);
                            kill.add(reachedState);
                        }
                    }
                    reachedSet.addAll(gen);
                    reachedSet.removeAll(kill);
                    waitlist.addAll(gen);
                    waitlist.removeAll(kill);
                    if (!stopOperator.stop(successorState, reachedSet.getReached(successorState), successorState.getPrecision()))
                    {
                        waitlist.add(successorState);
                        reachedSet.add(successorState);
                    }
                }
            }
            catch (Exception exception)
            {
                log.error("CPA run stopped for the following error: ", exception);
                waitlist.clear();
            }
        }
    }
}
