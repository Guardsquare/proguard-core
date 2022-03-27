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

import java.util.ArrayList;

/**
 * This {@link ListAbstractState} represents a list of {@link LatticeAbstractState}s with the semilattice operators lifted to the list.
 *
 * @author Dmitry Ivanov
 */
public class ListAbstractState<AbstractSpaceT extends LatticeAbstractState<AbstractSpaceT>>
    extends ArrayList<AbstractSpaceT>
    implements LatticeAbstractState<ListAbstractState<AbstractSpaceT>>
{

    /**
     * Create a list abstract state with initial capacity 0.
     */
    public ListAbstractState()
    {
        super(0);
    }

    /**
     * Create a list abstract state with selected initial capacity.
     *
     * @param initalCapacity initial capacity
     */
    public ListAbstractState(int initalCapacity)
    {
        super(initalCapacity);
    }

    // implementations for LatticeAbstractState

    @Override
    public ListAbstractState<AbstractSpaceT> join(ListAbstractState<AbstractSpaceT> abstractState)
    {
        if (this == abstractState)
        {
            return this;
        }
        ListAbstractState<AbstractSpaceT> shorterState;
        ListAbstractState<AbstractSpaceT> longerState;
        if (size() > abstractState.size())
        {
            shorterState = abstractState;
            longerState = this;
        }
        else
        {
            shorterState = this;
            longerState = abstractState;
        }
        ListAbstractState<AbstractSpaceT> joinResult = new ListAbstractState<>(longerState.size());
        joinResult.addAll(longerState);
        for (int i = 0; i < shorterState.size(); i++)
        {
            joinResult.set(i, shorterState.get(i).join(longerState.get(i)));
        }
        if (longerState.equals(joinResult))
        {
            return longerState;
        }
        return joinResult;
    }

    @Override
    public boolean isLessOrEqual(ListAbstractState<AbstractSpaceT> abstractState)
    {
        if (size() > abstractState.size())
        {
            return false;
        }
        for (int i = 0; i < size(); i++)
        {
            if (!get(i).isLessOrEqual(abstractState.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    // implementations for AbstractState

    @Override
    public ListAbstractState<AbstractSpaceT> copy()
    {
        ListAbstractState<AbstractSpaceT> copy = new ListAbstractState<>(size());
        copy.addAll(this);
        return copy;
    }

    /**
     * Returns the abstract state at {@code index}, if present, returns the {@code defaultState} otherwise.
     */
    public AbstractSpaceT getOrDefault(int index, AbstractSpaceT defaultState)
    {
        return index < size() ? get(index) : defaultState;
    }

    /**
     * Sets an element at {@code index} to {@code elem}. Extends its length and pads with {@code defaultState}, if necessary.
     */
    public AbstractSpaceT set(int index, AbstractSpaceT element, AbstractSpaceT defaultState)
    {
        while (index >= size())
        {
            add(defaultState);
        }
        return super.set(index, element);
    }
}
