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

import java.util.Comparator;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.edges.JvmCfaEdge;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.witness.JvmLocalVariableLocation;
import proguard.analysis.cpa.jvm.witness.JvmStaticFieldLocation;

/**
 * A {@link JvmAbstractState} for the reference CPA. Since the analysis may skip library methods which include some constructors, it treats missing information
 * about variables (i.e., initialization) as if they were references to distinct heap locations. Thus, we can approximate missing analysis of library constructors
 * and call site of the entry method. Thus, some aliasing can be missed and the analysis result becomes unsound.
 *
 * @author Dmitry Ivanov
 */
public class JvmReferenceAbstractState
    extends JvmAbstractState<SetAbstractState<Reference>>
{

    /**
     * Create a JVM reference abstract state.
     *
     * @param programLocation a CFA node
     * @param frame           a frame abstract state
     * @param heap            a heap abstract state
     * @param staticFields    a static field table
     */
    public JvmReferenceAbstractState(JvmCfaNode programLocation,
                                     JvmFrameAbstractState<SetAbstractState<Reference>> frame,
                                     JvmHeapAbstractState<SetAbstractState<Reference>> heap,
                                     MapAbstractState<String, SetAbstractState<Reference>> staticFields)
    {
        super(programLocation, frame, heap, staticFields);
    }

    // implementations for JvmAbstractState

    @Override
    public SetAbstractState<Reference> getVariableOrDefault(int index, SetAbstractState<Reference> defaultState)
    {
        if (index < frame.getLocalVariables().size())
        {
            return super.getVariableOrDefault(index, defaultState);
        }
        return new SetAbstractState<>(new Reference(getMethodEntryNode(), new JvmLocalVariableLocation(index)));
    }

    @Override
    public SetAbstractState<Reference> getStaticOrDefault(String fqn, SetAbstractState<Reference> defaultState)
    {
        if (staticFields.containsKey(fqn))
        {
            return super.getStaticOrDefault(fqn, defaultState);
        }
        return new SetAbstractState<>(new Reference(JvmUnknownCfaNode.INSTANCE, new JvmStaticFieldLocation(fqn)));
    }

    @Override
    public <T> SetAbstractState<Reference> getFieldOrDefault(T object, String descriptor, SetAbstractState<Reference> defaultValue)
    {
        return super.getFieldOrDefault(object, descriptor, defaultValue);
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmReferenceAbstractState join(JvmAbstractState<SetAbstractState<Reference>> abstractState)
    {
        JvmReferenceAbstractState answer = new JvmReferenceAbstractState(programLocation.equals(abstractState.getProgramLocation()) ? programLocation : topLocation,
                                                                         frame.join(abstractState.getFrame()),
                                                                         heap.join(abstractState.getHeap()),
                                                                         staticFields.join(abstractState.getStaticFields()));
        return equals(answer) ? this : answer;
    }

    // implementations for AbstractState

    @Override
    public JvmReferenceAbstractState copy()
    {
        return new JvmReferenceAbstractState(programLocation, frame.copy(), heap.copy(), staticFields.copy());
    }

    // private methods

    private JvmCfaNode getMethodEntryNode()
    {
        JvmCfaNode result = programLocation;
        while (result.getOffset() != 0)
        {
            result = result.getEnteringEdges().stream().map(JvmCfaEdge::getSource).min(Comparator.comparingInt(JvmCfaNode::getOffset)).get();
        }
        return result;
    }
}
