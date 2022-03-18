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

package proguard.analysis.cpa.domain.arg;

import java.util.Arrays;
import proguard.analysis.datastructure.callgraph.Call;
import proguard.classfile.Signature;
import proguard.analysis.cpa.bam.ReduceOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;

/**
 * This {@link ReduceOperator} delegates reduction to its inner reduce operator and
 * creates an ARG node to its output.
 *
 * @author Dmitry Ivanov
 */
public class ArgReduceOperator<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    implements ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT>
{

    protected final ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT> wrappedReduceOperator;
    protected final ArgAbstractStateFactory                        argAbstractStateFactory;

    /**
     * Create a wrapper reduce operator.
     *
     * @param wrappedReduceOperator   a reduce operator to be wrapped
     * @param argAbstractStateFactory an abstract state factory to wrap the operator result
     */
    public ArgReduceOperator(ReduceOperator<CfaNodeT, CfaEdgeT, SignatureT> wrappedReduceOperator, ArgAbstractStateFactory argAbstractStateFactory)
    {
        this.wrappedReduceOperator = wrappedReduceOperator;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for ReduceOperator

    @Override
    public ArgAbstractState reduce(AbstractState expandedInitialState, CfaNodeT blockEntryNode, Call call)
    {
        if (!(expandedInitialState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + expandedInitialState.getClass().getName());
        }
        ArgAbstractState expandedInitialArgState = (ArgAbstractState) expandedInitialState;
        AbstractState reducedState = wrappedReduceOperator.reduce(expandedInitialArgState.getWrappedState(),
                                                                  blockEntryNode,
                                                                  call);
        return reducedState == expandedInitialArgState.getWrappedState()
               ? expandedInitialArgState
               : argAbstractStateFactory.createArgAbstractState(reducedState,
                                                                Arrays.asList(expandedInitialArgState));
    }
}
