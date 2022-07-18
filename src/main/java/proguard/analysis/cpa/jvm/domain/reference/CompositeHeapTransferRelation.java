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
import java.util.Objects;

import proguard.analysis.cpa.interfaces.*;
import proguard.classfile.MethodSignature;
import proguard.analysis.cpa.defaults.LatticeAbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.transfer.JvmTransferRelation;

/**
 * A wrapper class around multiple {@link JvmTransferRelation}s applying them elementwise to {@link CompositeHeapJvmAbstractState}s.
 * The {@link CompositeHeapJvmAbstractState#REFERENCE_STATE_INDEX}th transfer relation must be {@link JvmReferenceTransferRelation} to match the structure of {@link CompositeHeapTransferRelation}.
 *
 * @author Dmitry Ivanov
 */
public class CompositeHeapTransferRelation
    implements WrapperTransferRelation,
               ProgramLocationDependentForwardTransferRelation<JvmCfaNode, JvmCfaEdge, MethodSignature>
{

    private final List<JvmTransferRelation<? extends AbstractState>> jvmTransferRelations;

    /**
     * Create a composite transfer relation from a list of transfer relations.
     *
     * @param jvmTransferRelations a list of {@link JvmTransferRelation}s, the {@code CompositeHeapJvmAbstractState.REFERENCE_INDEX}th transfer relation must be {@link JvmReferenceTransferRelation}
     */
    public CompositeHeapTransferRelation(List<JvmTransferRelation<? extends AbstractState>> jvmTransferRelations)
    {
        this.jvmTransferRelations = jvmTransferRelations;
    }

    // implementations for WrapperTransferRelation

    @Override
    public Iterable<? extends TransferRelation> getWrappedTransferRelations()
    {
        return jvmTransferRelations;
    }

    // implementations for ProgramLocationDependentTransferRelation

    @Override
    public CompositeHeapJvmAbstractState getEdgeAbstractSuccessor(AbstractState abstractState, JvmCfaEdge edge, Precision precision)
    {
        if (!(abstractState instanceof CompositeHeapJvmAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        CompositeHeapJvmAbstractState compositeState = (CompositeHeapJvmAbstractState) abstractState;
        Iterator<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> stateIterator = compositeState.getWrappedStates().iterator();
        List<JvmAbstractState<? extends LatticeAbstractState<? extends AbstractState>>> successorStates = new ArrayList<>(compositeState.getWrappedStates().size());
        jvmTransferRelations.forEach(tr -> successorStates.add((JvmAbstractState<? extends AbstractState>) tr.getEdgeAbstractSuccessor(stateIterator.next(), edge, precision)));
        if (successorStates.stream().anyMatch(Objects::isNull))
        {
            return null;
        }
        else
        {
            CompositeHeapJvmAbstractState result = new CompositeHeapJvmAbstractState(successorStates);
            result.updateHeapDependence();
            return result;
        }
    }
}
