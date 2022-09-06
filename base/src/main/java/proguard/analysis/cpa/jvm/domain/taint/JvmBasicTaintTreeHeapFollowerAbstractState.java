
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
import proguard.analysis.cpa.domain.taint.TaintAbstractState;
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
    extends JvmTreeHeapFollowerAbstractState<TaintAbstractState>
    implements JvmTaintHeapAbstractState
{

    /**
     * Create a taint follower heap abstract state.
     *
     * @param principal               the principal heap abstract state containing reference abstract states
     * @param defaultValue            the default value representing unknown values
     * @param referenceToNode         the mapping from references to heap nodes
     * @param mapAbstractStateFactory a map abstract state factory used for constructing the mapping from fields to values
     */
    public JvmBasicTaintTreeHeapFollowerAbstractState(JvmReferenceAbstractState principal, TaintAbstractState defaultValue, MapAbstractState<Reference, HeapNode<TaintAbstractState>> referenceToNode, MapAbstractStateFactory mapAbstractStateFactory)
    {
        super(principal, defaultValue, referenceToNode, mapAbstractStateFactory);
    }

    // implementations for JvmTaintHeapAbstractState

    @Override
    public <T> void taintObject(T object, TaintAbstractState value)
    {
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmBasicTaintTreeHeapFollowerAbstractState join(JvmHeapAbstractState<TaintAbstractState> abstractState)
    {
        JvmBasicTaintTreeHeapFollowerAbstractState other = (JvmBasicTaintTreeHeapFollowerAbstractState) abstractState;
        MapAbstractState<Reference, HeapNode<TaintAbstractState>> newReferenceToNode = referenceToNode.join(other.referenceToNode);
        if (referenceToNode == newReferenceToNode)
        {
            return this;
        }
        if (other.referenceToNode == newReferenceToNode)
        {
            return other;
        }
        return new JvmBasicTaintTreeHeapFollowerAbstractState(principal, defaultValue, newReferenceToNode, mapAbstractStateFactory);
    }

    // implementations for AbstractState

    @Override
    public JvmBasicTaintTreeHeapFollowerAbstractState copy()
    {
        return new JvmBasicTaintTreeHeapFollowerAbstractState(principal,
                                                              defaultValue,
                                                              referenceToNode.entrySet()
                                                                        .stream()
                                                                        .collect(Collectors.toMap(Entry::getKey,
                                                                                                  e -> e.getValue().copy(),
                                                                                                  HeapNode::join,
                                                                                                  mapAbstractStateFactory::createMapAbstractState)),
                                                              mapAbstractStateFactory);
    }
}
