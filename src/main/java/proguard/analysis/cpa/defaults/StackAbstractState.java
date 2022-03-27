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

import java.util.Stack;

/**
 * This {@link StackAbstractState} represents a stack of {@link LatticeAbstractState}s with the semilattice operators lifted to the stack.
 *
 * @author Dmitry Ivanov
 */
public class StackAbstractState<AbstractSpaceT extends LatticeAbstractState<AbstractSpaceT>>
    extends Stack<AbstractSpaceT>
    implements LatticeAbstractState<StackAbstractState<AbstractSpaceT>>
{

    // implementations for LatticeAbstractState

    @Override
    public StackAbstractState<AbstractSpaceT> join(StackAbstractState<AbstractSpaceT> abstractState)
    {
        if (this == abstractState)
        {
            return this;
        }
        StackAbstractState<AbstractSpaceT> joinResult = new StackAbstractState<>();
        StackAbstractState<AbstractSpaceT> shorterState;
        StackAbstractState<AbstractSpaceT> longerState;
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
        joinResult.addAll(longerState);
        for (int shortIndex = 0; shortIndex < shorterState.size(); ++shortIndex)
        {
            int longIndex = shortIndex + longerState.size() - shorterState.size();
            joinResult.set(longIndex,
                           shorterState.get(shortIndex).join(longerState.get(longIndex)));
        }
        if (longerState.equals(joinResult))
        {
            return longerState;
        }
        return joinResult;
    }

    @Override
    public boolean isLessOrEqual(StackAbstractState<AbstractSpaceT> abstractState)
    {
        int sizeDifference = abstractState.size() - size();
        if (sizeDifference < 0)
        {
            return false;
        }
        for (int i = 0; i < size(); i++)
        {
            if (!get(i).isLessOrEqual(abstractState.get(i + sizeDifference)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public StackAbstractState<AbstractSpaceT> copy()
    {
        StackAbstractState<AbstractSpaceT> copy = new StackAbstractState<>();
        copy.addAll(this);
        return copy;
    }

    /**
     * Removes the top of the stack and returns it.
     * If the stack is empty, it returns the {@code defaultState}.
     */
    public AbstractSpaceT popOrDefault(AbstractSpaceT defaultState)
    {
        return isEmpty() ? defaultState : pop();
    }

    /**
     * Returns the {@code index}th element from the top of the stack.
     * If the stack does not have enough elements, it throws an exception.
     */
    public AbstractSpaceT peek(int index)
    {
        int elementIndex = size() - 1 - index;
        if (elementIndex < 0)
        {
            throw new IllegalArgumentException("Operand stack index is out of bound (" + index + ")");
        }
        else
        {
            return get(elementIndex);
        }
    }

    /**
     * Returns the {@code index}th element from the top of the stack.
     * If the stack does not have enough elements, it returns the {@code defaultState}.
     */
    public AbstractSpaceT peekOrDefault(int index, AbstractSpaceT defaultState)
    {
        int elementIndex = size() - 1 - index;
        return elementIndex < 0 ? defaultState : get(elementIndex);
    }
}
