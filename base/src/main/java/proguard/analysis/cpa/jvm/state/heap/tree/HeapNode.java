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

package proguard.analysis.cpa.jvm.state.heap.tree;

import java.util.function.Function;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;

/**
 * The heap node represents an object or an array in the heap. The arrays do not distinguish between different indices and consider all their elements to be aliased.
 *
 * @author Dmitry Ivanov
 */
public class HeapNode<StateT extends LatticeAbstractState<StateT>>
    implements LatticeAbstractState<HeapNode<StateT>>
{

    private final MapAbstractState<String, StateT> fieldToAbstractState;

    /**
     * Create a heap node.
     */
    protected HeapNode()
    {
        this(new MapAbstractState<>());
    }

    protected HeapNode(MapAbstractState<String, StateT> fieldToAbstractState)
    {
        this.fieldToAbstractState = fieldToAbstractState;
    }

    // implementations for LatticeAbstractState

    @Override
    public HeapNode<StateT> join(HeapNode<StateT> abstractState)
    {
        MapAbstractState<String, StateT> newFieldToAbstractState = fieldToAbstractState.join(abstractState.fieldToAbstractState);
        return newFieldToAbstractState == fieldToAbstractState
               ? this
               : new HeapNode<>(newFieldToAbstractState);
    }

    @Override
    public boolean isLessOrEqual(HeapNode<StateT> abstractState)
    {
        return fieldToAbstractState.isLessOrEqual(abstractState.fieldToAbstractState);
    }

    @Override
    public HeapNode<StateT> copy()
    {
        return new HeapNode<>(fieldToAbstractState.copy());
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof HeapNode))
        {
            return false;
        }
        return fieldToAbstractState.equals(((HeapNode<StateT>) obj).fieldToAbstractState);
    }

    @Override
    public int hashCode()
    {
        return fieldToAbstractState.hashCode();
    }

    /**
     * Returns an abstract state for the corresponding field or the {@code defaultValue} if there is no entry.
     */
    public StateT getValueOrDefault(String descriptor, StateT defaultValue)
    {
        return fieldToAbstractState.getOrDefault(descriptor, defaultValue);
    }

    /**
     * Returns an abstract state for the corresponding field or computes it if there is no entry.
     */
    public StateT computeIfAbsent(String descriptor, Function<? super String, ? extends StateT> mappingFunction)
    {
        return fieldToAbstractState.computeIfAbsent(descriptor, mappingFunction);
    }

    /**
     * Joins the field value with the input {@code value}.
     */
    public void mergeValue(String descriptor, StateT value)
    {
        fieldToAbstractState.merge(descriptor, value, StateT::join);
    }

    /**
     * Sets the field to the input {@code value}.
     */
    public void setValue(String descriptor, StateT value)
    {
        fieldToAbstractState.put(descriptor, value);
    }
}
