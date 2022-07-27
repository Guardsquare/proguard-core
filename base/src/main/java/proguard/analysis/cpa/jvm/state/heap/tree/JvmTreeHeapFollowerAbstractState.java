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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmInstructionCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;

/**
 * This is a heap model for analyses that need to track the actual content of heap objects. This does not track references to these objects though,
 * it relies on {@link JvmTreeHeapPrincipalAbstractState} instead for finding out which field/variable references an object.
 *
 * @author Dmitry Ivanov
 */
public class JvmTreeHeapFollowerAbstractState<StateT extends LatticeAbstractState<StateT>>
    extends JvmTreeHeapAbstractState<StateT>
{

    private       JvmReferenceAbstractState principal;
    private final StateT                    defaultValue;

    /**
     * Create a follower heap abstract state.
     *
     * @param principal    the principal heap abstract state containing reference abstract states
     * @param defaultValue the default value representing unknown values
     */
    public JvmTreeHeapFollowerAbstractState(JvmReferenceAbstractState principal, StateT defaultValue)
    {
        this(principal, defaultValue, new MapAbstractState<>());
    }

    private JvmTreeHeapFollowerAbstractState(JvmReferenceAbstractState principal, StateT defaultValue, MapAbstractState<Reference, HeapNode<StateT>> referenceToNode)
    {
        super(referenceToNode);
        this.principal = principal;
        this.defaultValue = defaultValue;
    }

    // implementations for JvmHeapAbstractState

    @Override
    public StateT getField(StateT object, String descriptor, StateT defaultValue)
    {
        return getField(object instanceof SetAbstractState ? (SetAbstractState<Reference>) object : getReferenceAbstractState(),
                        descriptor,
                        defaultValue);
    }

    @Override
    public void setField(StateT object, String descriptor, StateT value)
    {
        setField(getReferenceAbstractState(), descriptor, value);
    }

    @Override
    public StateT getArrayElementOrDefault(StateT array, StateT index, StateT defaultValue)
    {
        return getArrayElementOrDefault(getReferenceAbstractState(),
                                        index,
                                        defaultValue);
    }

    @Override
    public void setArrayElement(StateT array, StateT index, StateT value)
    {
        setArrayElement(getReferenceAbstractState(),
                        index,
                        value);
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
    public void reduce(Optional<Set<Reference>> references)
    {
        Set<Reference> toKeep = ((JvmTreeHeapPrincipalAbstractState) principal.getHeap()).referenceToNode.keySet();

        if (toKeep.size() >= referenceToNode.size())
            return;

        referenceToNode.entrySet().removeIf(e -> !toKeep.contains(e.getKey()));
    }

// implementations for LatticeAbstractState

    @Override
    public JvmTreeHeapFollowerAbstractState<StateT> join(JvmHeapAbstractState<StateT> abstractState)
    {
        JvmTreeHeapFollowerAbstractState<StateT> other = (JvmTreeHeapFollowerAbstractState<StateT>) abstractState;
        MapAbstractState<Reference, HeapNode<StateT>> newReferenceToNode = referenceToNode.join(other.referenceToNode);
        if (referenceToNode.equals(newReferenceToNode))
        {
            return this;
        }
        if (other.referenceToNode.equals(newReferenceToNode))
        {
            return other;
        }
        return new JvmTreeHeapFollowerAbstractState<>(principal, defaultValue, newReferenceToNode);
    }

    @Override
    public boolean isLessOrEqual(JvmHeapAbstractState<StateT> abstractState)
    {
        return abstractState instanceof JvmTreeHeapFollowerAbstractState;
    }

    // implementations for AbstractState

    @Override
    public JvmTreeHeapFollowerAbstractState<StateT> copy()
    {
        return new JvmTreeHeapFollowerAbstractState<>(principal,
                                                      defaultValue,
                                                      referenceToNode.entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(Entry::getKey,
                                                                                              e -> e.getValue().copy(),
                                                                                              HeapNode::join,
                                                                                              MapAbstractState::new)));
    }

    public void setPrincipalState(JvmReferenceAbstractState principal)
    {
        this.principal = principal;
    }

    // private methods

    private SetAbstractState<Reference> getReferenceAbstractState()
    {
        // we abuse the fact that for all memory instructions the reference is the down-most operand on the stack
        // hence, we get the instruction from the CFA edge (for memory operations there is exactly one edge) and return its first (= deepest) operand
        return new JvmStackLocation(((JvmInstructionCfaEdge) principal.getProgramLocation()
                                                                      .getLeavingEdges()
                                                                      .stream()
                                                                      .findFirst()
                                                                      .get()).getInstruction().stackPopCount(principal.getProgramLocation()
                                                                                                                      .getClazz()) - 1).extractValueOrDefault(principal,
                                                                                                                                                              SetAbstractState.bottom);
    }
}
