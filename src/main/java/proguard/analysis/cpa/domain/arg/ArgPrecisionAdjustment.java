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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import proguard.analysis.cpa.defaults.PrecisionAdjustmentResult;
import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;

/**
 * This wrapper precision adjustment delegates precision selection to
 * its inner {@link PrecisionAdjustment} and wraps the adjusted {@link AbstractState}
 * with an ARG node.
 *
 * @author Dmitry Ivanov
 */
public class ArgPrecisionAdjustment
    implements PrecisionAdjustment
{

    protected final PrecisionAdjustment     wrappedPrecisionAdjustment;
    protected final ArgAbstractStateFactory argAbstractStateFactory;

    /**
     * Create a wrapper precision adjustment.
     *
     * @param wrappedPrecisionAdjustment a precision adjustment to be wrapped
     * @param argAbstractStateFactory    an ARG node factory
     */
    public ArgPrecisionAdjustment(PrecisionAdjustment wrappedPrecisionAdjustment, ArgAbstractStateFactory argAbstractStateFactory)
    {
        this.wrappedPrecisionAdjustment = wrappedPrecisionAdjustment;
        this.argAbstractStateFactory = argAbstractStateFactory;
    }

    // implementations for PrecisionAdjustment

    @Override
    public PrecisionAdjustmentResult prec(AbstractState abstractState, Precision precision, Collection<? extends AbstractState> reachedAbstractStates)
    {
        if (!(abstractState instanceof ArgAbstractState))
        {
            throw new IllegalArgumentException(getClass().getName() + " does not support " + abstractState.getClass().getName());
        }
        ArgAbstractState argAbstractState = (ArgAbstractState) abstractState;
        PrecisionAdjustmentResult wrappedPrecisionAdjustmentResult = wrappedPrecisionAdjustment.prec(argAbstractState.getWrappedState(),
                                                                                                     precision,
                                                                                                     reachedAbstractStates.stream()
                                                                                                                          .map(s -> ((ArgAbstractState) s).getWrappedState())
                                                                                                                          .collect(Collectors.toSet()));
        return new PrecisionAdjustmentResult(wrappedPrecisionAdjustmentResult.getAbstractState() == argAbstractState.getWrappedState()
                                             ? argAbstractState
                                             : argAbstractStateFactory.createArgAbstractState(wrappedPrecisionAdjustmentResult.getAbstractState(),
                                                                                              Collections.singletonList(argAbstractState)),
                                             wrappedPrecisionAdjustmentResult.getPrecision());
    }
}
