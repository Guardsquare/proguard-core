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
import proguard.analysis.cpa.bam.RebuildOperator;
import proguard.analysis.cpa.interfaces.AbstractState;

/**
 * This wrapper rebuild operator delegates rebuilding to its inner {@link RebuildOperator}
 * and wraps the output with an ARG node.
 *
 * @author Dmitry Ivanov
 */
public class ArgRebuildOperator
    implements RebuildOperator
{

    protected final RebuildOperator         wrappedRebuildOperator;
    protected final ArgAbstractStateFactory argAbstractStateFactory;

    /**
     * Create a wrapper rebuild operator for a given {@link RebuildOperator}.
     *
     * @param wrappedRebuildOperator  a rebuild operator to be wrapped
     * @param argAbstractStateFactory an abstract state factory for the result
     */
    public ArgRebuildOperator(RebuildOperator wrappedRebuildOperator,
                              ArgAbstractStateFactory argAbstractStateFactory)
    {
        this.wrappedRebuildOperator = wrappedRebuildOperator;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for RebuildOperator

    @Override
    public AbstractState rebuild(AbstractState predecessorCallState, AbstractState expandedOutputState)
    {
        if (!(predecessorCallState instanceof ArgAbstractState && expandedOutputState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + predecessorCallState.getClass().getName() + " and " + expandedOutputState);
        }
        ArgAbstractState predecessorCallArgState = (ArgAbstractState) predecessorCallState;
        ArgAbstractState expandedOutputArgState = (ArgAbstractState) expandedOutputState;
        AbstractState rebuiltAbstractState = wrappedRebuildOperator.rebuild(predecessorCallArgState.getWrappedState(),
                                                                            expandedOutputArgState.getWrappedState());
        return rebuiltAbstractState == expandedOutputArgState.getWrappedState()
               ? expandedOutputArgState
               : argAbstractStateFactory.createArgAbstractState(rebuiltAbstractState,
                                                                Arrays.asList(predecessorCallArgState,
                                                                              expandedOutputArgState));
    }
}
