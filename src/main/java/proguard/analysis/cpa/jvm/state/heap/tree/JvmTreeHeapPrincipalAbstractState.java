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
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmStackLocation;

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
     */
    public JvmTreeHeapPrincipalAbstractState()
    {
        super();
    }

    private JvmTreeHeapPrincipalAbstractState(MapAbstractState<Reference, HeapNode<SetAbstractState<Reference>>> referenceToNode)
    {
        super(referenceToNode);
    }

    // implementations for JvmHeapAbstractState

    @Override
    public SetAbstractState<Reference> getField(SetAbstractState<Reference> object, String descriptor, SetAbstractState<Reference> defaultValue)
    {
        return object.stream().reduce(new SetAbstractState<>(),
                                      (result, reference) -> referenceToNode.computeIfAbsent(reference, r -> new HeapNode<>())
                                                                            .computeIfAbsent(descriptor, d -> new SetAbstractState<>(reference)),
                                      SetAbstractState::join);
    }

    @Override
    public void setField(SetAbstractState<Reference> object, String descriptor, SetAbstractState<Reference> value)
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

    @Override
    public SetAbstractState<Reference> getArrayElementOrDefault(SetAbstractState<Reference> array, SetAbstractState<Reference> index, SetAbstractState<Reference> defaultValue)
    {
        return array.stream().reduce(new SetAbstractState<Reference>(),
                                     (result, reference) -> referenceToNode.computeIfAbsent(reference, r -> new HeapNode<>())
                                                                           .computeIfAbsent("[]", d -> new SetAbstractState<>(reference)),
                                     SetAbstractState::join);
    }

    @Override
    public void setArrayElement(SetAbstractState<Reference> array, SetAbstractState<Reference> index, SetAbstractState<Reference> value)
    {
        array.forEach(reference -> referenceToNode.computeIfAbsent(reference, r -> new HeapNode<>())
                                                  .mergeValue("[]", value));
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

    // implementations for LatticeAbstractState

    @Override
    public JvmTreeHeapPrincipalAbstractState join(JvmHeapAbstractState<SetAbstractState<Reference>> abstractState)
    {
        JvmTreeHeapPrincipalAbstractState other = (JvmTreeHeapPrincipalAbstractState) abstractState;
        MapAbstractState<Reference, HeapNode<SetAbstractState<Reference>>> newReferenceToNode = referenceToNode.join(other.referenceToNode);
        if (referenceToNode.equals(newReferenceToNode))
        {
            return this;
        }
        if (other.referenceToNode.equals(newReferenceToNode))
        {
            return other;
        }
        return new JvmTreeHeapPrincipalAbstractState(newReferenceToNode);
    }

    // implementations for AbstractState

    @Override
    public JvmTreeHeapPrincipalAbstractState copy()
    {
        return new JvmTreeHeapPrincipalAbstractState(referenceToNode.entrySet()
                                                                    .stream()
                                                                    .collect(Collectors.toMap(Entry::getKey,
                                                                                           e -> e.getValue().copy(),
                                                                                           HeapNode::join,
                                                                                           MapAbstractState::new)));
    }
}
