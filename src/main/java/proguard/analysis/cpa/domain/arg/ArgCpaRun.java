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

package proguard.analysis.cpa.domain.arg;

import java.util.Collection;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.CpaRun;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ConfigurableProgramAnalysis;
import proguard.analysis.cpa.interfaces.ReachedSet;

/**
 * This {@link CpaRun} wraps a given {@link CpaRun} with ARG creation.
 *
 * @author Dmitry Ivanov
 */
public class ArgCpaRun<CpaT extends ConfigurableProgramAnalysis, AbstractStateT extends AbstractState>
    extends CpaRun<ArgCpa, ArgAbstractState>
{

    protected final CpaRun<CpaT, AbstractStateT> wrappedCpaRun;
    protected final ArgAbstractStateFactory      argAbstractStateFactory;
    protected final ReachedSet                   reachedSet;

    /**
     * Create an ARG wrapper CPA run.
     *
     * @param wrappedCpaRun           a CPA run to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     * @param reachedSet              an empty reached set for the ARG
     */
    public ArgCpaRun(CpaRun<CpaT, AbstractStateT> wrappedCpaRun, ArgAbstractStateFactory argAbstractStateFactory, ReachedSet reachedSet)
    {
        this.wrappedCpaRun = wrappedCpaRun;
        this.argAbstractStateFactory = argAbstractStateFactory;
        this.reachedSet = reachedSet;
    }

    // implementations for CpaRun

    @Override
    public ArgCpa getCpa()
    {
        return cpa == null
               ? cpa = new ArgCpa(wrappedCpaRun.getCpa(), argAbstractStateFactory)
               : cpa;
    }

    @Override
    public Collection<ArgAbstractState> getInitialStates()
    {
        return wrappedCpaRun.getInitialStates().stream().map(argAbstractStateFactory::createArgAbstractState).collect(Collectors.toList());
    }

    @Override
    protected ReachedSet createReachedSet()
    {
        return reachedSet;
    }
}
