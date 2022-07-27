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
import java.util.stream.Collectors;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.defaults.AbstractWrapperState;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.ProgramLocationDependent;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapFollowerAbstractState;

/**
 * This {@link AbstractWrapperState} stores a {@link JvmReferenceAbstractState} having the {@link proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapPrincipalAbstractState}
 * and a sequence of {@link JvmAbstractState}s which may have {@link JvmTreeHeapFollowerAbstractState}s depending on the first abstract state. Join and copy are done elementwise
 * preserving the link between the heap models.
 *
 * <p>The composite abstract state must have the {@link JvmReferenceAbstractState} at its {@link #REFERENCE_STATE_INDEX}th position. This abstract state will be used as the principal heap model
 * for other abstract states.</p>
 *
 * @author Dmitry Ivanov
 */
public class CompositeHeapJvmAbstractState
    extends AbstractWrapperState
    implements LatticeAbstractState<CompositeHeapJvmAbstractState>,
               ProgramLocationDependent<JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    // the position of the {@link JvmReferenceAbstractState} among the wrapped {@link JvmAbstractState}
    public static final int                                                                              REFERENCE_STATE_INDEX = 0;
    private final        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> jvmAbstractStates;

    /**
     * Create a composite abstract state from a list of JVM abstract states.
     *
     * @param jvmAbstractStates a list of {@link JvmAbstractState}s, must contain a {@link JvmReferenceAbstractState} at its {@code REFERENCE_INDEX}th position
     */
    public CompositeHeapJvmAbstractState(List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> jvmAbstractStates)
    {
        if (!(jvmAbstractStates.get(REFERENCE_STATE_INDEX) instanceof JvmReferenceAbstractState))
        {
            throw new IllegalArgumentException("The abstract state at index " + REFERENCE_STATE_INDEX + " must be a reference abstract state");
        }
        this.jvmAbstractStates = jvmAbstractStates;
    }

    /**
     * Returns the state at the specified position in the composite state.
     */
    public JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>> getStateByIndex(int index)
    {
        return jvmAbstractStates.get(index);
    }

    public void updateHeapDependence()
    {
        jvmAbstractStates.stream()
                         .map(s -> s.getHeap())
                         .filter(JvmTreeHeapFollowerAbstractState.class::isInstance)
                         .forEach(h -> ((JvmTreeHeapFollowerAbstractState) h)
                             .setPrincipalState((JvmReferenceAbstractState) jvmAbstractStates.get(CompositeHeapJvmAbstractState.REFERENCE_STATE_INDEX)));
    }

    // implementations for AbstractWrapperState

    @Override
    public List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> getWrappedStates()
    {
        return jvmAbstractStates;
    }

    // implementations for LatticeAbstractState

    @Override
    public CompositeHeapJvmAbstractState join(CompositeHeapJvmAbstractState abstractState)
    {
        if (jvmAbstractStates.size() != abstractState.jvmAbstractStates.size())
        {
            throw new IllegalArgumentException("Trying to join two abstract state sequences of different lengths");
        }
        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> resultStates = new ArrayList<>(jvmAbstractStates.size());
        for (int i = 0; i < jvmAbstractStates.size(); i++)
        {
            JvmAbstractState<? extends AbstractState> state1 = jvmAbstractStates.get(i);
            JvmAbstractState<? extends AbstractState> state2 = abstractState.jvmAbstractStates.get(i);
            resultStates.add(state1.join(state1.getClass().cast(state2)));
        }
        CompositeHeapJvmAbstractState result = new CompositeHeapJvmAbstractState(resultStates);
        return equals(result)
               ? this
               : abstractState.equals(result)
                 ? abstractState
                 : result;
    }

    @Override
    public boolean isLessOrEqual(CompositeHeapJvmAbstractState abstractState)
    {
        if (jvmAbstractStates.size() != abstractState.jvmAbstractStates.size())
        {
            throw new IllegalArgumentException("Trying to compare two abstract state sequences of different lengths");
        }
        for (int i = 0; i < jvmAbstractStates.size(); i++)
        {
            JvmAbstractState<? extends AbstractState> state1 = jvmAbstractStates.get(i);
            JvmAbstractState<? extends AbstractState> state2 = abstractState.jvmAbstractStates.get(i);
            if (!state1.isLessOrEqual(state1.getClass().cast(state2)))
            {
                return false;
            }
        }
        return true;
    }

    // implementations for ProgramLocationDependent

    @Override
    public JvmCfaNode getProgramLocation()
    {
        return jvmAbstractStates.get(0).getProgramLocation();
    }

    @Override
    public void setProgramLocation(JvmCfaNode programLocation)
    {
        jvmAbstractStates.forEach(s -> s.setProgramLocation(programLocation));
    }

    // implementations for AbstractState


    @Override
    public AbstractState getStateByName(String name)
    {
        switch (name)
        {
            case "Reference":
                return jvmAbstractStates.get(REFERENCE_STATE_INDEX);
            default:
                return jvmAbstractStates.get(jvmAbstractStates.size() - 1);
        }
    }

    @Override
    public AbstractState copy()
    {
        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> newJvmAbstractStates = jvmAbstractStates.stream().map(JvmAbstractState::copy).collect(Collectors.toList());
        CompositeHeapJvmAbstractState copy = new CompositeHeapJvmAbstractState(newJvmAbstractStates);
        copy.updateHeapDependence();
        return copy;
    }
}
