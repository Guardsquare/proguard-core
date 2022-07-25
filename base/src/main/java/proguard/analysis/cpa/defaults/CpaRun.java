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

import java.util.Collection;
import proguard.analysis.cpa.algorithms.CpaAlgorithm;
import proguard.analysis.cpa.interfaces.AbortOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ReachedSet;
import proguard.analysis.cpa.interfaces.Waitlist;

/**
 * This abstract wrapper class runs the selected {@link ConfigurableProgramAnalysis} and returns the {@link ReachedSet}.
 *
 * @author Dmitry Ivanov
 */
public abstract class CpaRun<CpaT extends ConfigurableProgramAnalysis, AbstractStateT extends AbstractState>
{

    protected CpaT           cpa;
    protected AbortOperator  abortOperator = NeverAbortOperator.INSTANCE;

    /**
     * Sets up the {@link CpaAlgorithm}, runs it, and returns the {@link ReachedSet} with the result of the analysis.
     */
    public ReachedSet execute()
    {
        CpaAlgorithm cpaAlgorithm = new CpaAlgorithm(getCpa());
        Waitlist waitList         = createWaitlist();
        ReachedSet reachedSet     = createReachedSet();
        Collection<AbstractStateT> initialStates = getInitialStates();
        waitList.addAll(initialStates);
        reachedSet.addAll(initialStates);
        cpaAlgorithm.run(reachedSet, waitList, getAbortOperator());
        return reachedSet;
    }

    /**
     * Returns an empty {@link ReachedSet}.
     */
    protected ReachedSet createReachedSet()
    {
        return new DefaultReachedSet();
    }

    /**
     * Returns an empty {@link Waitlist}.
     */
    protected Waitlist createWaitlist()
    {
        return new BreadthFirstWaitlist();
    }

    /**
     * Returns a collection of initial {@link AbstractState}s.
     */
    public abstract Collection<AbstractStateT> getInitialStates();

    /**
     * Returns the CPA.
     */
    public CpaT getCpa()
    {
        return cpa;
    }

    /**
     * Returns the abort operator.
     */
    public AbortOperator getAbortOperator()
    {
        return abortOperator;
    }
}
