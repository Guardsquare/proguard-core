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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This {@link SetAbstractState} represents a set with the subset ordering.
 *
 * @author Dmitry Ivanov
 */
public class SetAbstractState<T>
    extends HashSet<T>
    implements LatticeAbstractState<SetAbstractState<T>>
{

    public static SetAbstractState bottom = new SetAbstractState();

    /**
     * Create a set abstract state from its elements.
     *
     * @param items an array of elements
     */
    public SetAbstractState(T... items)
    {
        this(Arrays.asList(items));
    }

    /**
     * Create a set abstract state from a collection.
     *
     * @param c a collection of elements
     */
    public SetAbstractState(Collection<? extends T> c)
    {
        super(c);
    }

    // implementations for LatticeAbstractState

    @Override
    public SetAbstractState<T> join(SetAbstractState<T> abstractState)
    {
        SetAbstractState<T> result = abstractState.copy();
        result.addAll(this);
        return result.isLessOrEqual(this)
               ? this
               : result.isLessOrEqual(abstractState)
                 ? abstractState
                 : result;
    }

    @Override
    public boolean isLessOrEqual(SetAbstractState<T> abstractState)
    {
        return abstractState.containsAll(this);
    }

    // implementations for AbstractState

    @Override
    public SetAbstractState<T> copy()
    {
        return (SetAbstractState<T>) super.clone();
    }
}
