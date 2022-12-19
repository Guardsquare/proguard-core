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

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;

/**
 * The {@link JvmTaintAbstractState} is a {@link JvmAbstractState} with features specific to taint analysis.
 *
 * @author Dmitry Ivanov
 */
public class JvmTaintAbstractState
    extends JvmAbstractState<SetAbstractState<JvmTaintSource>>
{

    /**
     * Create a taint JVM abstract state.
     *
     * @param programLocation a CFA node
     * @param frame           a frame abstract state
     * @param heap            a heap abstract state
     * @param staticFields    a static field table
     */
    public JvmTaintAbstractState(JvmCfaNode programLocation,
                                 JvmFrameAbstractState<SetAbstractState<JvmTaintSource>> frame,
                                 JvmHeapAbstractState<SetAbstractState<JvmTaintSource>> heap,
                                 MapAbstractState<String, SetAbstractState<JvmTaintSource>> staticFields)
    {
        super(programLocation, frame, heap, staticFields);
    }

    /**
     * Adds transitively taints from {@code value} to all fields of {@code object}.
     */
    public <T> void setObjectTaint(T object, SetAbstractState<JvmTaintSource> value)
    {
        if (!(heap instanceof JvmTaintTreeHeapFollowerAbstractState))
        {
            return;
        }
        ((JvmTaintTreeHeapFollowerAbstractState) heap).taintObject(object, value);
    }

    // implementations for LatticeAbstractState

    @Override
    public JvmTaintAbstractState join(JvmAbstractState<SetAbstractState<JvmTaintSource>> abstractState)
    {
        JvmTaintAbstractState answer = new JvmTaintAbstractState(programLocation.equals(abstractState.getProgramLocation()) ? programLocation : topLocation,
                                                                 frame.join(abstractState.getFrame()),
                                                                 heap.join(abstractState.getHeap()),
                                                                 staticFields.join(abstractState.getStaticFields()));
        return equals(answer) ? this : answer;
    }

    // implementations for AbstractState

    @Override
    public JvmTaintAbstractState copy()
    {
        return new JvmTaintAbstractState(programLocation, frame.copy(), heap.copy(), staticFields.copy());
    }
}
