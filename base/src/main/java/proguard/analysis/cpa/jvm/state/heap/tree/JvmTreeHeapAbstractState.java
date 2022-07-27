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

import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;

/**
 * The tree heap model represents the memory as a map from references to objects or arrays ({@link HeapNode}s) which may refer other heap nodes in their fields.
 *
 * @author Dmitry Ivanov
 */
public abstract class JvmTreeHeapAbstractState<StateT extends LatticeAbstractState<StateT>>
    implements JvmHeapAbstractState<StateT>
{

    protected final MapAbstractState<Reference, HeapNode<StateT>> referenceToNode;

    /**
     * Create an empty tree heap abstract state.
     */
    public JvmTreeHeapAbstractState()
    {
        this(new MapAbstractState<>());
    }

    /**
     * Create an tree heap abstract state from a given memory layout.
     *
     * @param referenceToNode a mapping from references to their objects/arrays
     */
    protected JvmTreeHeapAbstractState(MapAbstractState<Reference, HeapNode<StateT>> referenceToNode)
    {
        this.referenceToNode = referenceToNode;
    }

    // implementations for JvmHeapAbstractState

    /**
     * Returns a join over all fields aliased by the input {@link SetAbstractState}. The {@code defaultValue} is used when there is no information available.
     */
    protected StateT getField(SetAbstractState<Reference> object, String descriptor, StateT defaultValue)
    {
        return object.stream().reduce(defaultValue,
                                      (result, reference) -> result.join(referenceToNode.containsKey(reference)
                                                                         ? referenceToNode.get(reference).getValueOrDefault(descriptor, defaultValue)
                                                                         : defaultValue),
                                      StateT::join);
    }

    /**
     * Sets the field value to the given one if the reference is unambiguous, joins otherwise.
     */
    protected void setField(SetAbstractState<Reference> object, String descriptor, StateT value)
    {
        if (object.size() <= 1)
        {
            object.forEach(reference -> referenceToNode.computeIfAbsent(reference, r -> new HeapNode<>())
                                                       .setValue(descriptor, value));
        }
        else
        {
            object.forEach(reference -> referenceToNode.computeIfAbsent(reference, r -> new HeapNode<>())
                                                       .mergeValue(descriptor, value));
        }
    }

    /**
     * Returns a join over all arrays aliased by the input {@link SetAbstractState}. The {@code defaultValue} is used when there is no information available.
     */
    protected StateT getArrayElementOrDefault(SetAbstractState<Reference> array, StateT index, StateT defaultValue)
    {
        return array.stream().reduce(defaultValue,
                                     (result, reference) -> result.join(referenceToNode.containsKey(reference)
                                                                        ? referenceToNode.get(reference).getValueOrDefault("[]", defaultValue)
                                                                        : defaultValue),
                                     StateT::join);
    }

    /**
     * Joins the array elements with the given one for all aliases.
     */
    protected void setArrayElement(SetAbstractState<Reference> array, StateT index, StateT value)
    {
        array.forEach(reference -> referenceToNode.computeIfAbsent(reference, r -> new HeapNode<>())
                                                  .mergeValue("[]", value));
    }

    /**
     * Expands the state with all the entries from another heap state with reference not already known by the state.
     */
    @Override
    public void expand(JvmHeapAbstractState<StateT> otherState)
    {
        if (!(otherState instanceof JvmTreeHeapAbstractState))
        {
            throw new IllegalArgumentException("The other state should be a JvmTreeHeapAbstractState");
        }

        ((JvmTreeHeapAbstractState<StateT>) otherState).referenceToNode.forEach(referenceToNode::putIfAbsent);
    }

    // implementations for LatticeAbstractState

    @Override
    public boolean isLessOrEqual(JvmHeapAbstractState<StateT> abstractState)
    {
        return abstractState instanceof JvmTreeHeapAbstractState
               && referenceToNode.isLessOrEqual(((JvmTreeHeapAbstractState<StateT>) abstractState).referenceToNode);
    }

    // implementations for Object

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof JvmTreeHeapAbstractState))
        {
            return false;
        }
        return referenceToNode.equals(((JvmTreeHeapAbstractState) obj).referenceToNode);
    }

    @Override
    public int hashCode()
    {
        return referenceToNode.hashCode();
    }
}
