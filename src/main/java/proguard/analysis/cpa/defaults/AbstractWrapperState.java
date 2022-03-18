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

/**
 * This {@link AbstractState} wraps an arbitrary number of other {@link AbstractState}s.
 *
 * @author Dmitry Ivanov
 */
public abstract class AbstractWrapperState
    implements AbstractState
{

    /**
     * Returns the wrapped abstract states.
     */
    public abstract Iterable<? extends AbstractState> getWrappedStates();

    // implementations for AbstractState

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof AbstractWrapperState))
        {
            return false;
        }
        AbstractWrapperState other = (AbstractWrapperState) obj;
        return getWrappedStates().equals(other.getWrappedStates());
    }

    @Override
    public int hashCode()
    {
        return getWrappedStates().hashCode();
    }
}
