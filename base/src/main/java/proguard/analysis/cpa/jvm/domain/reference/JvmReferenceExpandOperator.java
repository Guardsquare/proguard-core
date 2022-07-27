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

import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.JvmCfa;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultExpandOperator;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;

/**
 * This expand operator behaves the same as the {@link JvmDefaultExpandOperator} but operates on {@link JvmReferenceAbstractState}s.
 *
 * @author Dmitry Ivanov
 */
public class JvmReferenceExpandOperator
    extends JvmDefaultExpandOperator<SetAbstractState<Reference>>
{

    /**
     * Create the expand operator for the JVM reference analysis.
     *
     * @param cfa the control flow automaton of the analyzed program
     */
    public JvmReferenceExpandOperator(JvmCfa cfa)
    {
        this(cfa, true);
    }

    /**
     * Create the expand operator for the JVM reference analysis.
     *
     * @param cfa        the control flow automaton of the analyzed program
     * @param expandHeap whether expansion of the heap is performed
     */
    public JvmReferenceExpandOperator(JvmCfa cfa, boolean expandHeap)
    {
        super(cfa, expandHeap);
    }

    // implementations for JvmAbstractStateFactory

    @Override
    public JvmReferenceAbstractState createJvmAbstractState(JvmCfaNode programLocation,
                                                            JvmFrameAbstractState<SetAbstractState<Reference>> frame,
                                                            JvmHeapAbstractState<SetAbstractState<Reference>> heap,
                                                            MapAbstractState<String, SetAbstractState<Reference>> staticFields)
    {
        return new JvmReferenceAbstractState(programLocation, frame, heap, staticFields);
    }
}
