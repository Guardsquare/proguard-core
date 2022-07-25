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

import java.util.Arrays;
import java.util.List;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * This {@link AbstractWrapperState} wraps a single {@link AbstractState}
 * and delegates the precision getter to it.
 *
 * @author Dmitry Ivanov
 */
public class AbstractSingleWrapperState
    extends AbstractWrapperState
{

    protected final AbstractState wrappedAbstractState;

    /**
     * Create a single wrapper abstract state around the given state.
     *
     * @param wrappedAbstractState an inner abstract state
     */
    public AbstractSingleWrapperState(AbstractState wrappedAbstractState)
    {
        this.wrappedAbstractState = wrappedAbstractState;
    }

    /**
     * Returns the wrapped abstract state.
     */
    public AbstractState getWrappedState()
    {
        return wrappedAbstractState;
    }

    // implementations for AbstractWrapperState

    @Override
    public List<AbstractState> getWrappedStates()
    {
        return Arrays.asList(wrappedAbstractState);
    }

    // implementations for AbstractState

    @Override
    public Precision getPrecision()
    {
        return wrappedAbstractState.getPrecision();
    }

    @Override
    public AbstractSingleWrapperState copy()
    {
        return new AbstractSingleWrapperState(wrappedAbstractState.copy());
    }
}
