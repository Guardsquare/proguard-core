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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * A wrapper class around multiple {@link ReduceOperator}s applying them elementwise to {@link CompositeHeapJvmAbstractState}s.
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

        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> reducedStates = new ArrayList<>(((CompositeHeapJvmAbstractState) expandedInitialState).getWrappedStates()
                                                                                                                                                                              .size());
        Iterator<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> expandedStateIterator = ((CompositeHeapJvmAbstractState) expandedInitialState).getWrappedStates()
                                                                                                                                                                          .iterator();

        wrappedReducedOperators.forEach(ro -> reducedStates.add((JvmAbstractState<? extends AbstractState>) ro.reduce(expandedStateIterator.next(), blockEntryNode, call)));
        CompositeHeapJvmAbstractState result = new CompositeHeapJvmAbstractState(reducedStates);
        result.updateHeapDependence();
        return result;
    }
}
