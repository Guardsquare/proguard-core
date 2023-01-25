
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

package proguard.analysis.cpa.jvm.domain.taint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.HeapNode;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapPrincipalAbstractState;
import proguard.analysis.cpa.state.MapAbstractStateFactory;

/**
 * This is a {@link JvmTreeHeapFollowerAbstractState} with the possibility of object tainting.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintTreeHeapFollowerAbstractState
    extends JvmTreeHeapFollowerAbstractState<SetAbstractState<JvmTaintSource>>
    implements JvmTaintHeapAbstractState
{

    /**
     * Create a taint follower heap abstract state.
     *
     * @param principal                       the principal heap abstract state containing reference abstract states
     * @param defaultValue                    the default value representing unknown values
     * @param referenceToNode                 the mapping from references to heap nodes
     * @param heapMapAbstractStateFactory     a map abstract state factory used for constructing the mapping from references to objects
     * @param heapNodeMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    public JvmTaintTreeHeapFollowerAbstractState(JvmReferenceAbstractState principal,
                                                 SetAbstractState<JvmTaintSource> defaultValue,
                                                 MapAbstractState<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> referenceToNode,
                                                 MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> heapMapAbstractStateFactory,
                                                 MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> heapNodeMapAbstractStateFactory)
    {
        super(principal, defaultValue, referenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory);
    }

    // implementations for JvmTaintHeapAbstractState

    @Override
    public <T> void taintObject(T object, SetAbstractState<JvmTaintSource> value)
    {
        setField(object, "", value);
    }

    // implementations for JvmTreeHeapFollowerAbstractState

    @Override
    public <T> SetAbstractState<JvmTaintSource> getFieldOrDefault(T object, String fqn, SetAbstractState<JvmTaintSource> defaultValue)
    {
        return super.getFieldOrDefault(object, fqn, defaultValue.join(super.getFieldOrDefault(object, "", SetAbstractState.bottom)));
    }

    @Override
    public <T> SetAbstractState<JvmTaintSource> getArrayElementOrDefault(T array, SetAbstractState<JvmTaintSource> index, SetAbstractState<JvmTaintSource> defaultValue)
    {
        return super.getArrayElementOrDefault(array, index, defaultValue.join(super.getFieldOrDefault(array, "", SetAbstractState.bottom)));
    }

    // implementations for JvmTreeHeapAbstractState

    @Override
    protected void assignField(SetAbstractState<Reference> object, String descriptor, SetAbstractState<JvmTaintSource> value)
    {
        // ordinary field assignment
        if (!descriptor.equals(""))
        {
            super.assignField(object, descriptor, value);
            return;
        }
        // object tainting
        taintObjects(getReachableReferences(object, (JvmTreeHeapPrincipalAbstractState) principal.getHeap()), referenceToObject, value);
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmTaintTreeHeapFollowerAbstractState join(JvmHeapAbstractState<SetAbstractState<JvmTaintSource>> abstractState)
    {
        JvmTaintTreeHeapFollowerAbstractState other = (JvmTaintTreeHeapFollowerAbstractState) abstractState;
        MapAbstractState<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> newReferenceToNode = referenceToObject.join(other.referenceToObject);
        if (referenceToObject == newReferenceToNode)
        {
            return this;
        }
        if (other.referenceToObject == newReferenceToNode)
        {
            return other;
        }
        propagateObjectTaint(referenceToObject, newReferenceToNode, other.principal.getHeap());
        propagateObjectTaint(other.referenceToObject, newReferenceToNode, principal.getHeap());
        return new JvmTaintTreeHeapFollowerAbstractState(principal, defaultValue, newReferenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory);
    }

    // implementations for AbstractState

    @Override
    public JvmTaintTreeHeapFollowerAbstractState copy()
    {
        return new JvmTaintTreeHeapFollowerAbstractState(principal,
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

    // private methods

    private SetAbstractState<Reference> getReachableReferences(Set<Reference> references, JvmTreeHeapPrincipalAbstractState principal)
    {
        SetAbstractState<Reference> result = new SetAbstractState<>();
        Deque<Reference> worklist = new ArrayDeque<>();
        references.forEach(worklist::push);
        // collect all affected references
        while (!worklist.isEmpty())
        {
            Reference reference = worklist.pop();
            result.add(reference);
            Optional.ofNullable(principal.getHeapNode(reference)).ifPresent(n -> n.values()
                                                                                  .stream()
                                                                                  .flatMap(Set::stream)
                                                                                  .filter(r -> !result.contains(r))
                                                                                  .forEach(r ->
                                                                                           {
                                                                                               result.add(r);
                                                                                               worklist.push(r);
                                                                                           }));
        }
        return result;
    }

    private void taintObjects(SetAbstractState<Reference> references, Map<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> referenceToNode, SetAbstractState<JvmTaintSource> value)
    {
        // taint all fields as values
        references.stream()
                  .map(referenceToNode::get)
                  .filter(Objects::nonNull)
                  .forEach(n -> n.replaceAll((k, v) -> v.join(value)));
        // object taint all fields
        references.stream()
                  .map(SetAbstractState<Reference>::new)
                  .forEach(r -> mergeField(r, "", value));
    }

    private void propagateObjectTaint(MapAbstractState<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> leftReferenceToNode,
                                      MapAbstractState<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> joinedReferenceToNode,
                                      JvmHeapAbstractState<SetAbstractState<Reference>> rightPrincipalHeap)
    {
        leftReferenceToNode.entrySet()
                           .stream()
                           .filter(e -> e.getValue()
                                         .containsKey("")) // get tainted objects
                           .collect(Collectors.groupingBy(e -> e.getValue()
                                                                .get(""))) // group by taint
                           .forEach((taint, group) -> taintObjects(getReachableReferences(group.stream()
                                                                                               .flatMap(heapEntry -> joinedReferenceToNode.get(heapEntry.getKey())
                                                                                                                                          .entrySet()
                                                                                                                                          .stream()
                                                                                                                                          .filter(o -> !heapEntry.getValue()
                                                                                                                                                                 .containsKey(o.getKey())) // get newly discovered fields
                                                                                                                                          .map(o ->
                                                                                                                                               {
                                                                                                                                                   // taint the field value
                                                                                                                                                   o.setValue(o.getValue().join(taint));
                                                                                                                                                   // collect field reference for further tainting
                                                                                                                                                   return rightPrincipalHeap.getFieldOrDefault(new SetAbstractState<>(heapEntry.getKey()),
                                                                                                                                                                                               o.getKey(),
                                                                                                                                                                                               (SetAbstractState<Reference>) SetAbstractState.bottom);
                                                                                                                                               }))
                                                                                               .flatMap(Set::stream)
                                                                                               .collect(Collectors.toSet()),
                                                                                          (JvmTreeHeapPrincipalAbstractState) rightPrincipalHeap),
                                                                   joinedReferenceToNode,
                                                                   taint));
    }
}
