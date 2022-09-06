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
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.MethodSignature;

/**
 * A wrapper class around multiple {@link ExpandOperator}s applying them elementwise to {@link CompositeHeapJvmAbstractState}s.
 *
 * Also recovers all the heap nodes that have been discarded at the call site.
 *
 * @author Dmitry Ivanov
 */
public class JvmCompositeHeapExpandOperator
    implements ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    protected final List<? extends ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>> wrappedExpandOperators;

    /**
     * Create a composite expand operator from a list of expand operators.
     *
     * @param wrappedExpandOperators a list of expand operators with the order matching the structure of the target {@link JvmReferenceAbstractState}s
     */
    public JvmCompositeHeapExpandOperator(List<? extends ExpandOperator<JvmCfaNode, JvmCfaEdge, MethodSignature>> wrappedExpandOperators)
    {
        this.wrappedExpandOperators = wrappedExpandOperators;
    }

    // Implementations for ExpandOperator

    @Override
    public CompositeHeapJvmAbstractState expand(AbstractState expandedInitialState, AbstractState reducedExitState, JvmCfaNode blockEntryNode, Call call)
    {
        if (!(expandedInitialState instanceof CompositeHeapJvmAbstractState))
        {
            throw new IllegalArgumentException("The operator works on composite JVM states, states of type " + expandedInitialState.getClass().getName() + " are not supported");
        }

        if (!(reducedExitState instanceof CompositeHeapJvmAbstractState))
        {
            throw new IllegalArgumentException("The operator works on composite JVM states, states of type " + reducedExitState.getClass().getName() + " are not supported");
        }

        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> expandedStates = new ArrayList<>(((CompositeHeapJvmAbstractState) expandedInitialState).getWrappedStates()
                                                                                                                                                                               .size());
        Iterator<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> expandedStateIterator = ((CompositeHeapJvmAbstractState) expandedInitialState).getWrappedStates()
                                                                                                                                                                          .iterator();
        Iterator<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> reducedStateIterator = ((CompositeHeapJvmAbstractState) reducedExitState).getWrappedStates().iterator();

        wrappedExpandOperators.forEach(eo -> expandedStates.add((JvmAbstractState<? extends AbstractState>) eo.expand(expandedStateIterator.next(),
                                                                                                                      reducedStateIterator.next(),
                                                                                                                      blockEntryNode,
                                                                                                                      call)));

        CompositeHeapJvmAbstractState result = new CompositeHeapJvmAbstractState(expandedStates);
        result.updateHeapDependence();
        return result;
    }
}
