
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

import java.util.Map.Entry;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.JvmReferenceAbstractState;
import proguard.analysis.cpa.jvm.domain.reference.Reference;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.HeapNode;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;
import proguard.analysis.cpa.state.MapAbstractStateFactory;

/**
 * This is a {@link JvmTreeHeapFollowerAbstractState} without object tainting.
 *
 * @author Dmitry Ivanov
 */
public class JvmBasicTaintTreeHeapFollowerAbstractState
    extends JvmTreeHeapFollowerAbstractState<SetAbstractState<JvmTaintSource>>
    implements JvmTaintHeapAbstractState
{

    /**
     * Create a taint follower heap abstract state.
     *
     * @param principal                     the principal heap abstract state containing reference abstract states
     * @param defaultValue                  the default value representing unknown values
     * @param referenceToNode               the mapping from references to heap nodes
     * @param heapMapAbstractStateFactory   a map abstract state factory used for constructing the mapping from references to objects
     * @param objectMapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    public JvmBasicTaintTreeHeapFollowerAbstractState(JvmReferenceAbstractState principal,
                                                      SetAbstractState<JvmTaintSource> defaultValue, MapAbstractState<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> referenceToNode,
                                                      MapAbstractStateFactory<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> heapMapAbstractStateFactory,
                                                      MapAbstractStateFactory<String, SetAbstractState<JvmTaintSource>> objectMapAbstractStateFactory)
    {
        super(principal, defaultValue, referenceToNode, heapMapAbstractStateFactory, objectMapAbstractStateFactory);
    }

    // implementations for JvmTaintHeapAbstractState

    @Override
    public <T> void taintObject(T object, SetAbstractState<JvmTaintSource> value)
    {
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmBasicTaintTreeHeapFollowerAbstractState join(JvmHeapAbstractState<SetAbstractState<JvmTaintSource>> abstractState)
    {
        JvmBasicTaintTreeHeapFollowerAbstractState other = (JvmBasicTaintTreeHeapFollowerAbstractState) abstractState;
        MapAbstractState<Reference, HeapNode<SetAbstractState<JvmTaintSource>>> newReferenceToNode = referenceToObject.join(other.referenceToObject);
        if (referenceToObject == newReferenceToNode)
        {
            return this;
        }
        if (other.referenceToObject == newReferenceToNode)
        {
            return other;
        }
        return new JvmBasicTaintTreeHeapFollowerAbstractState(principal, defaultValue, newReferenceToNode, heapMapAbstractStateFactory, heapNodeMapAbstractStateFactory);
    }

    // implementations for AbstractState

    @Override
    public JvmBasicTaintTreeHeapFollowerAbstractState copy()
    {
        return new JvmBasicTaintTreeHeapFollowerAbstractState(principal,
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
}
