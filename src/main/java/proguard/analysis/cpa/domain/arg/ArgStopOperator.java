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
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.StopOperator;

/**
 * This {@link StopOperator} delegates the decision to the wrapped stop operator.
 *
 * @author Dmitry Ivanov
 */
public class ArgStopOperator
    implements StopOperator
{

    protected final StopOperator wrappedStopOperator;

    /**
     * Create a wrapper stop operator around the given {@link StopOperator}.

     * @param wrappedStopOperator a stop operator to be wrapped.
     */
    public ArgStopOperator(StopOperator wrappedStopOperator)
    {
        this.wrappedStopOperator = wrappedStopOperator;
    }

    // implementations for StopOperator

    @Override
    public boolean stop(AbstractState abstractState, Collection<? extends AbstractState> reachedAbstractStates, Precision precision)
    {
        if (!(abstractState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        return wrappedStopOperator.stop(((ArgAbstractState) abstractState).getWrappedState(),
                                        reachedAbstractStates.stream().map(s -> ((ArgAbstractState) s).getWrappedState()).collect(Collectors.toSet()),
                                        precision);
    }
}
