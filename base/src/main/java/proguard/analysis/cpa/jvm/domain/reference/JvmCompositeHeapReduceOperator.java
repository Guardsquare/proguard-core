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

package proguard.analysis.cpa.jvm.domain.reference;

import static proguard.analysis.cpa.jvm.domain.reference.CompositeHeapJvmAbstractState.REFERENCE_STATE_INDEX;

import java.util.ArrayList;
import java.util.List;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;

/**
 * A wrapper class around multiple {@link ReduceOperator}s applying them elementwise to {@link CompositeHeapJvmAbstractState}s.
 *
 * Also discards from the heap state all nodes not in a subtree of references in method's call arguments or static variables.
 *
 * @author Dmitry Ivanov
 */
public class JvmCompositeHeapReduceOperator
    implements ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    private final List<? extends ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>> wrappedReducedOperators;

    /**
     * Create a composite reduce operator from a list of reduce operators.
     *
     * @param wrappedReducedOperators a list of reduce operators with the order matching the structure of the target {@link JvmReferenceAbstractState}s
     */
    public JvmCompositeHeapReduceOperator(List<? extends ReduceOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>> wrappedReducedOperators)
    {
        this.wrappedReducedOperators = wrappedReducedOperators;
    }

    // Implementations for ReduceOperator

    @Override
    public CompositeHeapJvmAbstractState reduce(AbstractState expandedInitialState, JvmCfaNode blockEntryNode, Call call)
    {
        if (!(expandedInitialState instanceof CompositeHeapJvmAbstractState))
        {
            throw new IllegalArgumentException("The operator works on composite JVM states, states of type " + expandedInitialState.getClass().getName() + " are not supported");
        }

        int stateSize = ((CompositeHeapJvmAbstractState) expandedInitialState).getWrappedStates()
                                                                              .size();
        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> reducedStates = new ArrayList<>(stateSize);

        // reduce reference state
        JvmReferenceAbstractState reducedReferenceState = (JvmReferenceAbstractState) wrappedReducedOperators
            .get(REFERENCE_STATE_INDEX)
            .reduce(((CompositeHeapJvmAbstractState) expandedInitialState).getStateByIndex(REFERENCE_STATE_INDEX), blockEntryNode, call);
        reducedStates.add(REFERENCE_STATE_INDEX, reducedReferenceState);

        // assign principal state to followers and reduce them
        // assigning the new reduced principal state is necessary to let the followers know what to discard
        for (int i = 0; i < stateSize; i++)
        {
            if (i == REFERENCE_STATE_INDEX)
            {
                continue;
            }

            JvmAbstractState<?> state = ((CompositeHeapJvmAbstractState) expandedInitialState).getStateByIndex(i).copy();
            ((JvmTreeHeapFollowerAbstractState<?>) state.getHeap()).setPrincipalState(reducedReferenceState);
            JvmAbstractState<?> reducedState = (JvmAbstractState<?>) wrappedReducedOperators.get(i)
                                                                                            .reduce(state, blockEntryNode, call);
            reducedStates.add(i, reducedState);
        }

        CompositeHeapJvmAbstractState result = new CompositeHeapJvmAbstractState(reducedStates);
        result.updateHeapDependence();
        return result;
    }
}
