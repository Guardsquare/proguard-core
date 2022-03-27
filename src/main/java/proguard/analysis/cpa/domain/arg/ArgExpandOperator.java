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
import proguard.analysis.cpa.bam.ExpandOperator;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.CfaEdge;
import proguard.analysis.cpa.interfaces.CfaNode;

/**
 * This wrapper expand operator delegates expansion to its inner {@link ExpandOperator}
 * and wraps the output with an ARG node.
 *
 * @author Dmitry Ivanov
 */
public class ArgExpandOperator<CfaNodeT extends CfaNode<CfaEdgeT, SignatureT>, CfaEdgeT extends CfaEdge<CfaNodeT>, SignatureT extends Signature>
    implements ExpandOperator<CfaNodeT, CfaEdgeT, SignatureT>
{

    protected final ExpandOperator<CfaNodeT, CfaEdgeT, SignatureT> wrappedExpandOperator;
    protected final ArgAbstractStateFactory                        argAbstractStateFactory;

    /**
     * Create a wrapper expand operator.
     *
     * @param wrappedExpandOperator   an expand operator to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     */
    public ArgExpandOperator(ExpandOperator<CfaNodeT, CfaEdgeT, SignatureT> wrappedExpandOperator,
                             ArgAbstractStateFactory argAbstractStateFactory)
    {
        this.wrappedExpandOperator = wrappedExpandOperator;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for ExpandOperator

    @Override
    public ArgAbstractState expand(AbstractState expandedInitialState, AbstractState reducedExitState, CfaNodeT blockEntryNode, Call call)
    {
        if (!(expandedInitialState instanceof ArgAbstractState && reducedExitState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + expandedInitialState.getClass().getName() + " and " + reducedExitState.getClass().getName());
        }
        ArgAbstractState expandedInitialArgState = (ArgAbstractState) expandedInitialState;
        ArgAbstractState reducedExitArgState = (ArgAbstractState) reducedExitState;
        AbstractState expandedState = wrappedExpandOperator.expand(expandedInitialArgState.getWrappedState(),
                                                                   reducedExitArgState.getWrappedState(),
                                                                   blockEntryNode,
                                                                   call);
        return expandedState == reducedExitArgState.getWrappedState()
               ? reducedExitArgState
               : argAbstractStateFactory.createArgAbstractState(expandedState,
                                                                Arrays.asList(expandedInitialArgState,
                                                                              reducedExitArgState));
    }
}
