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

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmMemoryLocation;
import proguard.analysis.cpa.state.MapAbstractStateFactory;

/**
 * This is a heap model for analyses that need to track the actual content of heap objects. This does not track references to these objects though,
 * it relies on {@link JvmTreeHeapPrincipalAbstractState} instead for finding out which field/variable references an object.
 *
 * @author Dmitry Ivanov
 */
public class JvmTreeHeapFollowerAbstractState<StateT extends LatticeAbstractState<StateT>>
    extends JvmTreeHeapAbstractState<StateT>
{

    protected JvmReferenceAbstractState principal;

    /**
     * Create a follower heap abstract state.
     *
     * @param principal                       the principal heap abstract state containing reference abstract states
     * @param defaultValue                    the default value representing unknown values
     * @param referenceToNode                 the mapping from references to heap nodes
     * @param heapMapAbstractStateFactory     a map abstract state factory used for constructing the mapping from references to objects
     * @param heapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    public JvmTreeHeapFollowerAbstractState(JvmReferenceAbstractState principal,
                                            StateT defaultValue,
                                            MapAbstractState<Reference, HeapNode<StateT>> referenceToNode,
                                            MapAbstractStateFactory<Reference, HeapNode<StateT>> heapMapAbstractStateFactory,
                                            MapAbstractStateFactory<String, StateT> heapNodeMapAbstractStateFactory)
    {
        super(referenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory, defaultValue);
        this.principal = principal;
    }

    // implementations for JvmHeapAbstractState

    @Override
    public <T> StateT getFieldOrDefault(T object, String fqn, StateT defaultValue)
    {
        if (object instanceof JvmMemoryLocation)
        {
            return getField(getReferenceAbstractState((JvmMemoryLocation) object),
                            fqn,
                            defaultValue);
        }
        if (object instanceof SetAbstractState)
        {
            return getField((SetAbstractState<Reference>) object,
                            fqn,
                            defaultValue);
        }
        throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), object.getClass().getName()));
    }

    @Override
    public <T> void setField(T object, String fqn, StateT value)
    {
        if (object instanceof JvmMemoryLocation)
        {
            assignField(getReferenceAbstractState((JvmMemoryLocation) object),
                        fqn,
                        value);
            return;
        }
        if (object instanceof SetAbstractState)
        {
            assignField((SetAbstractState<Reference>) object,
                        fqn,
                        value);
            return;
        }
        throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), object.getClass().getName()));
    }

    @Override
    public <T> StateT getArrayElementOrDefault(T array, StateT index, StateT defaultValue)
    {
        if (array instanceof JvmMemoryLocation)
        {
            return getArrayElementOrDefault(getReferenceAbstractState((JvmMemoryLocation) array),
                                            index,
                                            defaultValue);
        }
        if (array instanceof SetAbstractState)
        {
            return getArrayElementOrDefault((SetAbstractState<Reference>) array,
                                            index,
                                            defaultValue);
        }
        throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), array.getClass().getName()));
    }

    @Override
    public <T> void setArrayElement(T array, StateT index, StateT value)
    {
        if (array instanceof JvmMemoryLocation)
        {
            setArrayElement(getReferenceAbstractState((JvmMemoryLocation) array),
                            index,
                            value);
            return;
        }
        if (array instanceof SetAbstractState)
        {
            setArrayElement((SetAbstractState<Reference>) array,
                            index,
                            value);
            return;
        }
        throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), array.getClass().getName()));
    }

    @Override
    public StateT newObject(String className, JvmCfaNode creationCite)
    {
        return defaultValue;
    }

    @Override
    public StateT newArray(String type, List<StateT> dimensions, JvmCfaNode creationCite)
    {
        return defaultValue;
    }

    /**
     * Removes all the nodes not present in the principal model.
     *
     * @param references unused
     */
    @Override
    public void reduce(Set<Object> references)
    {
        Set<Reference> toKeep = ((JvmTreeHeapPrincipalAbstractState) principal.getHeap()).referenceToObject.keySet();
        if (toKeep.size() >= referenceToObject.size())
        {
            return;
        }
        referenceToObject.keySet().retainAll(toKeep);
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmTreeHeapFollowerAbstractState<StateT> join(JvmHeapAbstractState<StateT> abstractState)
    {
        JvmTreeHeapFollowerAbstractState<StateT> other = (JvmTreeHeapFollowerAbstractState<StateT>) abstractState;
        MapAbstractState<Reference, HeapNode<StateT>> newReferenceToNode = referenceToObject.join(other.referenceToObject);
        if (referenceToObject == newReferenceToNode)
        {
            return this;
        }
        if (other.referenceToObject == newReferenceToNode)
        {
            return other;
        }
        return new JvmTreeHeapFollowerAbstractState<>(principal, defaultValue, newReferenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory);
    }

    // implementations for AbstractState

    @Override
    public JvmTreeHeapFollowerAbstractState<StateT> copy()
    {
        return new JvmTreeHeapFollowerAbstractState<>(principal,
                                                      defaultValue,
                                                      referenceToObject.entrySet()
                                                                       .stream()
                                                                       .collect(Collectors.toMap(Entry::getKey,
                                                                                               e -> e.getValue().copy(),
                                                                                               HeapNode::join,
                                                                                               heapMapAbstractStateFactory::createMapAbstractState)),
                                                      heapMapAbstractStateFactory,
                                                      heapNodeMapAbstractStateFactory);
    }

    /**
     * Sets the {@code principal} abstract state containing references.
     */
    public void setPrincipalState(JvmReferenceAbstractState principal)
    {
        this.principal = principal;
    }

    public SetAbstractState<Reference> getReferenceAbstractState(JvmMemoryLocation principalMemoryLocation)
    {
        return principalMemoryLocation.extractValueOrDefault(principal,
                                                             SetAbstractState.bottom);
    }
}
