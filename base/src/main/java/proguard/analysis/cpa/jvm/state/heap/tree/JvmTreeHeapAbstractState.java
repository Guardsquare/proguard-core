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
import proguard.analysis.cpa.state.MapAbstractStateFactory;

/**
 * The tree heap model represents the memory as a map from references to objects or arrays ({@link HeapNode}s) which may refer other heap nodes in their fields.
 *
 * @author Dmitry Ivanov
 */
public abstract class JvmTreeHeapAbstractState<StateT extends LatticeAbstractState<StateT>>
    implements JvmHeapAbstractState<StateT>
{

    protected final MapAbstractState<Reference, HeapNode<StateT>>        referenceToObject;
    protected final MapAbstractStateFactory<String, StateT>              heapNodeMapAbstractStateFactory;
    protected final MapAbstractStateFactory<Reference, HeapNode<StateT>> heapMapAbstractStateFactory;
    protected final StateT                                               defaultValue;

    /**
     * Create a tree heap abstract state from a given memory layout.
     *
     * @param referenceToObject               a mapping from references to their objects/arrays
     * @param heapMapAbstractStateFactory     a map abstract state factory used for constructing the mapping from references to objects
     * @param heapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     * @param defaultValue                    a default value for undefined fields
     */
    protected JvmTreeHeapAbstractState(MapAbstractState<Reference, HeapNode<StateT>>        referenceToObject,
                                       MapAbstractStateFactory<Reference, HeapNode<StateT>> heapMapAbstractStateFactory,
                                       MapAbstractStateFactory<String, StateT>              heapNodeMapAbstractStateFactory,
                                       StateT                                               defaultValue)
    {
        this.referenceToObject               = referenceToObject;
        this.heapMapAbstractStateFactory     = heapMapAbstractStateFactory;
        this.heapNodeMapAbstractStateFactory = heapNodeMapAbstractStateFactory;
        this.defaultValue                    = defaultValue;
    }

    /**
     * Returns a join over all fields aliased by the input {@link SetAbstractState}. The {@code defaultValue} is used when there is no information available.
     */
    protected StateT getField(SetAbstractState<Reference> object, String descriptor, StateT defaultValue)
    {
        return object.stream().reduce(this.defaultValue,
                                      (result, reference) -> result.join(referenceToObject.containsKey(reference)
                                                                         ? referenceToObject.get(reference).getOrDefault(descriptor, defaultValue)
                                                                         : defaultValue),
                                      StateT::join);
    }

    /**
     * Assigns the field value to the given one if the reference is unambiguous, joins otherwise.
     */
    protected void assignField(SetAbstractState<Reference> object, String descriptor, StateT value)
    {
        if (object.size() <= 1)
        {
            setField(object, descriptor,value);
        }
        else
        {
            mergeField(object, descriptor,value);
        }
    }

    /**
     * Merges the field value to the given one.
     */
    protected void mergeField(SetAbstractState<Reference> object, String descriptor, StateT value)
    {
        object.forEach(reference -> referenceToObject.computeIfAbsent(reference, r -> new HeapNode<>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                     .merge(descriptor, value));
    }

    /**
     * Replaces the field value with the given one.
     */
    protected void setField(SetAbstractState<Reference> object, String descriptor, StateT value)
    {
        object.forEach(reference -> referenceToObject.computeIfAbsent(reference, r -> new HeapNode<>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                     .put(descriptor, value));
    }

    /**
     * Returns a join over all arrays aliased by the input {@link SetAbstractState}. The {@code defaultValue} is used when there is no information available.
     */
    protected StateT getArrayElementOrDefault(SetAbstractState<Reference> array, StateT index, StateT defaultValue)
    {
        return array.stream().reduce(this.defaultValue,
                                     (result, reference) -> result.join(referenceToObject.containsKey(reference)
                                                                        ? referenceToObject.get(reference).getOrDefault("[]", defaultValue)
                                                                        : defaultValue),
                                     StateT::join);
    }

    /**
     * Joins the array elements with the given one for all aliases.
     */
    protected void setArrayElement(SetAbstractState<Reference> array, StateT index, StateT value)
    {
        array.forEach(reference -> referenceToObject.computeIfAbsent(reference, r -> new HeapNode<>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                    .merge("[]", value));
    }

    // implementations for JvmHeapAbstractState

    /**
     * Returns the heap node for the given {@code reference}.
     */
    public HeapNode<StateT> getHeapNode(Reference reference)
    {
        return referenceToObject.get(reference);
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

        ((JvmTreeHeapAbstractState<StateT>) otherState).referenceToObject.forEach(referenceToObject::putIfAbsent);
    }

    // implementations for LatticeAbstractState

    @Override
    public boolean isLessOrEqual(JvmHeapAbstractState<StateT> abstractState)
    {
        return abstractState instanceof JvmTreeHeapAbstractState
               && referenceToObject.isLessOrEqual(((JvmTreeHeapAbstractState<StateT>) abstractState).referenceToObject);
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
        return referenceToObject.equals(((JvmTreeHeapAbstractState<StateT>) obj).referenceToObject);
    }

    @Override
    public int hashCode()
    {
        return referenceToObject.hashCode();
    }
}
