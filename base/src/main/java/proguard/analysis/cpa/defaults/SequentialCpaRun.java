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

import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ReachedSet;

/**
 * This {@link CpaRun} wraps another {@code InputCpaRunT} and allows a following {@code CpaT}
 * to be constructed using the output of the former run.
 *
 * @author Dmitry Ivanov
 */
public abstract class SequentialCpaRun<CpaT extends ConfigurableProgramAnalysis,
                                       AbstractStateT extends AbstractState,
                                       InputCpaRunT extends CpaRun>
    extends CpaRun<CpaT, AbstractStateT>
{

    protected final InputCpaRunT inputCpaRun;
    protected       ReachedSet   inputReachedSet;
    protected       ReachedSet   outputReachedSet;

    /**
     * Create a CPA run which accepts the reached set of another CPA run.
     *
     * @param inputCpaRun the inner CPA run
     */
    public SequentialCpaRun(InputCpaRunT inputCpaRun)
    {
        this.inputCpaRun = inputCpaRun;
    }

    // implementations for CpaRun

    @Override
    public ReachedSet execute()
    {
        inputReachedSet = inputCpaRun.execute();
        return outputReachedSet = super.execute();
    }

    public ReachedSet getOutputReachedSet()
    {
        return outputReachedSet == null ? outputReachedSet = execute() : outputReachedSet;
    }

    public InputCpaRunT getInputCpaRun()
    {
        return inputCpaRun;
    }
}
