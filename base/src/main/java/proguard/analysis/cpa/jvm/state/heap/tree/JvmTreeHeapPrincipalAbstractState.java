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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;
import proguard.analysis.cpa.state.MapAbstractStateFactory;

/**
 * This is a self-sufficient heap model in the sense that it contains references necessary for addressing. When a memory location is accessed, it is created on-the-fly and is assumed to contain
 * a distinct reference. Currently, such creation aliases all top level fields with deeper ones. For instance, if the object {@code o} is created on-the-fly, its field {@code o.f} will be
 * aliased with {@code o.f.f}.
 *
 * @author Dmitry Ivanov
 */
public class JvmTreeHeapPrincipalAbstractState
    extends JvmTreeHeapAbstractState<SetAbstractState<Reference>>
{

    /**
     * Create an empty principal heap model.
     *
     * @param heapMapAbstractStateFactory     a map abstract state factory used for constructing the mapping from references to objects
     * @param heapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    public JvmTreeHeapPrincipalAbstractState(MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>> heapMapAbstractStateFactory,
                                             MapAbstractStateFactory<String, SetAbstractState<Reference>> heapNodeMapAbstractStateFactory)
    {
        this(heapMapAbstractStateFactory.createMapAbstractState(), heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory);
    }

    private JvmTreeHeapPrincipalAbstractState(MapAbstractState<Reference, HeapNode<SetAbstractState<Reference>>> referenceToNode,
                                              MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<Reference>>> heapMapAbstractStateFactory,
                                              MapAbstractStateFactory<String, SetAbstractState<Reference>> heapNodeMapAbstractStateFactory)
    {
        super(referenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory, new SetAbstractState<>());
    }

    // implementations for JvmHeapAbstractState

    @Override
    public <T> SetAbstractState<Reference> getFieldOrDefault(T object, String fqn, SetAbstractState<Reference> defaultValue)
    {
        if (!(object instanceof SetAbstractState))
        {
            throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), object.getClass().getName()));
        }
        return ((SetAbstractState<Reference>) object).stream()
                                                     .reduce(new SetAbstractState<>(),
                                                             (result, reference) -> referenceToObject.computeIfAbsent(reference,
                                                                                                                    r -> new HeapNode<SetAbstractState<Reference>>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                                                                     .computeIfAbsent(fqn,
                                                                                                                    d -> new SetAbstractState<>(reference)),
                                                             SetAbstractState::join);
    }

    @Override
    public <T> void setField(T object, String fqn, SetAbstractState<Reference> value)
    {
        if (!(object instanceof SetAbstractState))
        {
            throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), object.getClass().getName()));
        }
        SetAbstractState<Reference> objectReference = (SetAbstractState<Reference>) object;
        if (objectReference.size() <= 1)
        {
            objectReference.forEach(reference -> referenceToObject.computeIfAbsent(reference, r -> new HeapNode<SetAbstractState<Reference>>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                                  .put(fqn, value));
        }
        else
        {
            objectReference.forEach(reference -> referenceToObject.computeIfAbsent(reference, r -> new HeapNode<SetAbstractState<Reference>>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                                  .merge(fqn, value));
        }
    }

    @Override
    public <T> SetAbstractState<Reference> getArrayElementOrDefault(T array, SetAbstractState<Reference> index, SetAbstractState<Reference> defaultValue)
    {
        if (!(array instanceof SetAbstractState))
        {
            throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), array.getClass().getName()));
        }
        return ((SetAbstractState<Reference>) array).stream().reduce(new SetAbstractState<>(),
                                                                     (result, reference) -> referenceToObject.computeIfAbsent(reference,
                                                                                                                            r -> new HeapNode<SetAbstractState<Reference>>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                                                                             .computeIfAbsent("[]",
                                                                                                                            d -> new SetAbstractState<>(reference)),
                                                                     SetAbstractState::join);
    }

    @Override
    public <T> void setArrayElement(T array, SetAbstractState<Reference> index, SetAbstractState<Reference> value)
    {
        if (!(array instanceof SetAbstractState))
        {
            throw new IllegalStateException(String.format("%s does not support %s as reference type", getClass().getName(), array.getClass().getName()));
        }
        ((SetAbstractState<Reference>) array).forEach(reference -> referenceToObject.computeIfAbsent(reference,
                                                                                                   r -> new HeapNode<SetAbstractState<Reference>>(heapNodeMapAbstractStateFactory.createMapAbstractState()))
                                                                                    .merge("[]", value));
    }

    @Override
    public SetAbstractState<Reference> newObject(String className, JvmCfaNode creationCite)
    {
        return new SetAbstractState<>(new Reference(creationCite, new JvmStackLocation(0)));
    }

    @Override
    public SetAbstractState<Reference> newArray(String type, List<SetAbstractState<Reference>> dimensions, JvmCfaNode creationCite)
    {
        return new SetAbstractState<>(new Reference(creationCite, new JvmStackLocation(0)));
    }

    /**
     * Get all the references to nodes that have been created in a {@link JvmStaticFieldLocation}.
     */
    public Set<Object> getStaticCreationReferences()
    {
        return referenceToObject.keySet()
                                .stream()
                                .filter(ref -> ref.creationSite instanceof JvmStaticFieldLocation)
                                .collect(Collectors.toSet());
    }

    /**
     * Slices the principal heap tree starting from the specified roots.
     */
    @Override
    public void reduce(Set<Object> roots)
    {
        Deque<Object> worklist = new ArrayDeque<>(roots);
        Set<Object> discoveredReferences = new HashSet<>(roots);

        // collect references in the subtree of the roots
        while (!worklist.isEmpty())
        {
            Object reference = worklist.pop();
            HeapNode<SetAbstractState<Reference>> node = referenceToObject.get(reference);

            if (node == null)
            {
                continue;
            }

            node.values().forEach(n -> n.stream()
                                        .filter(discoveredReferences::add)
                                        .forEach(worklist::add));
        }

        referenceToObject.entrySet().removeIf(e -> !discoveredReferences.contains(e.getKey()));
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmTreeHeapPrincipalAbstractState join(JvmHeapAbstractState<SetAbstractState<Reference>> abstractState)
    {
        JvmTreeHeapPrincipalAbstractState other = (JvmTreeHeapPrincipalAbstractState) abstractState;
        MapAbstractState<Reference, HeapNode<SetAbstractState<Reference>>> newReferenceToNode = referenceToObject.join(other.referenceToObject);
        if (referenceToObject.equals(newReferenceToNode))
        {
            return this;
        }
        if (other.referenceToObject.equals(newReferenceToNode))
        {
            return other;
        }
        return new JvmTreeHeapPrincipalAbstractState(newReferenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory);
    }

    // implementations for AbstractState

    @Override
    public JvmTreeHeapPrincipalAbstractState copy()
    {
        return new JvmTreeHeapPrincipalAbstractState(referenceToObject.entrySet()
                                                                      .stream()
                                                                      .collect(Collectors.toMap(Entry::getKey,
                                                                                              e -> e.getValue().copy(),
                                                                                              HeapNode::join,
                                                                                              heapMapAbstractStateFactory::createMapAbstractState)),
                                                     heapMapAbstractStateFactory,
                                                     heapNodeMapAbstractStateFactory);
    }
}
