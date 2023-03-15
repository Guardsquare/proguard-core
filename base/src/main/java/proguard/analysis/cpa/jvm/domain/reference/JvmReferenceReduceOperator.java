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

import java.util.Set;
import proguard.analysis.cpa.defaults.MapAbstractState;
import proguard.analysis.cpa.defaults.SetAbstractState;
import proguard.analysis.cpa.jvm.cfa.nodes.JvmCfaNode;
import proguard.analysis.cpa.jvm.operators.JvmDefaultReduceOperator;
import proguard.analysis.cpa.jvm.state.JvmFrameAbstractState;
import proguard.analysis.cpa.jvm.state.heap.JvmHeapAbstractState;
import proguard.analysis.cpa.jvm.state.heap.tree.JvmTreeHeapPrincipalAbstractState;

/**
 * This reduce operator behaves the same as the {@link JvmDefaultReduceOperator} but operates on {@link JvmReferenceAbstractState}s.
 *
 * @author Dmitry Ivanov
 */
public class JvmReferenceReduceOperator
    extends JvmDefaultReduceOperator<SetAbstractState<Reference>>
{

    /**
     * Create the reduce operator for the JVM reference analysis.
     */
    public JvmReferenceReduceOperator()
    {
        this(true);
    }

    /**
     * Create the reduce operator for the JVM reference analysis.
     *
     * @param reduceHeap whether reduction of the heap is performed
     */
    public JvmReferenceReduceOperator(boolean reduceHeap)
    {
        super(reduceHeap);
    }

    // implementations for JvmDefaultReduceOperator

    /**
     * Performs reduction of the {@link JvmTreeHeapPrincipalAbstractState} keeping just the portion of the tree rooted at
     * references in static fields and parameters.
     */
    @Override
    protected void reduceHeap(JvmHeapAbstractState<SetAbstractState<Reference>> heap,
                              JvmFrameAbstractState<SetAbstractState<Reference>> reducedFrame,
                              MapAbstractState<String, SetAbstractState<Reference>> reducedStaticFields)
    {
            Set<Object> roots = ((JvmTreeHeapPrincipalAbstractState) heap).getStaticCreationReferences();
            reducedFrame.getLocalVariables().forEach(roots::addAll);
            reducedStaticFields.values().forEach(roots::addAll);

            heap.reduce(roots);
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
