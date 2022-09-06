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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;

/**
 * The heap node represents an object or an array in the heap. The arrays do not distinguish between different indices and consider all their elements to be aliased.
 *
 * @author Dmitry Ivanov
 */
public class HeapNode<StateT extends LatticeAbstractState<StateT>>
    implements Map<String, StateT>,
               LatticeAbstractState<HeapNode<StateT>>
{

    private final MapAbstractState<String, StateT> fieldToAbstractState;

    /**
     * Create a heap node form a map abstract state.
     */
    public HeapNode(MapAbstractState<String, StateT> fieldToAbstractState)
    {
        this.fieldToAbstractState = fieldToAbstractState;
    }

    // implementations for Map

    @Override
    public int size()
    {
        return fieldToAbstractState.size();
    }

    @Override
    public boolean isEmpty()
    {
        return fieldToAbstractState.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return fieldToAbstractState.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return fieldToAbstractState.containsValue(value);
    }

    @Override
    public StateT get(Object key)
    {
        return fieldToAbstractState.get(key);
    }

    @Nullable
    @Override
    public StateT put(String key, StateT value)
    {
        return fieldToAbstractState.put(key, value);
    }

    @Override
    public StateT remove(Object key)
    {
        return fieldToAbstractState.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends StateT> m)
    {
        fieldToAbstractState.putAll(m);
    }

    @Override
    public void clear()
    {
        fieldToAbstractState.clear();
    }

    @NotNull
    @Override
    public Set<String> keySet()
    {
        return fieldToAbstractState.keySet();
    }

    @Override
    public Collection<StateT> values()
    {
        return fieldToAbstractState.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, StateT>> entrySet()
    {
        return fieldToAbstractState.entrySet();
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
     * Joins the field value with the input {@code value}.
     */
    public void merge(String descriptor, StateT value)
    {
        fieldToAbstractState.merge(descriptor, value, StateT::join);
    }
}
