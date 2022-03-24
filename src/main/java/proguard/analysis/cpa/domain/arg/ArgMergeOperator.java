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

import java.util.stream.Collectors;
import java.util.stream.Stream;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.MergeOperator;
import proguard.analysis.cpa.interfaces.Precision;

/**
 * This wrapper merge operator delegates merging to its inner {@link MergeOperator}
 * and wraps the output with an ARG node.
 *
 * @author Dmitry Ivanov
 */
public class ArgMergeOperator
    implements MergeOperator
{

    protected final MergeOperator           wrappedMergeOperator;
    protected final ArgAbstractStateFactory argAbstractStateFactory;

    /**
     * Create a wrapper merge operator.
     *
     * @param wrappedMergeOperator    a merge operator to be wrapped
     * @param argAbstractStateFactory an ARG node factory
     */
    public ArgMergeOperator(MergeOperator wrappedMergeOperator,
                            ArgAbstractStateFactory argAbstractStateFactory)
    {
        this.wrappedMergeOperator = wrappedMergeOperator;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for MergeOperator

    @Override
    public ArgAbstractState merge(AbstractState abstractState1, AbstractState abstractState2, Precision precision)
    {
        if (!(abstractState1 instanceof ArgAbstractState && abstractState2 instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState1.getClass().getName() + " and " + abstractState2.getClass().getName());
        }
        ArgAbstractState abstractArgState1 = (ArgAbstractState) abstractState1;
        ArgAbstractState abstractArgState2 = (ArgAbstractState) abstractState2;
        AbstractState mergedState = wrappedMergeOperator.merge(abstractArgState1.getWrappedState(),
                                                               abstractArgState2.getWrappedState(),
                                                               precision);
        return mergedState.equals(abstractArgState2.getWrappedState())
               ? abstractArgState2
               : mergedState.equals(abstractArgState1.getWrappedState())
                 ? abstractArgState1
                 : argAbstractStateFactory.createArgAbstractState(mergedState,
                                                                  Stream.concat(abstractArgState1.parents.stream(),
                                                                                abstractArgState2.parents.stream())
                                                                        .collect(Collectors.toList()));
    }
}
