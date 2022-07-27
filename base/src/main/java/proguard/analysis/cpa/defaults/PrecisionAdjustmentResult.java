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

package proguard.analysis.cpa.defaults;

import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.interfaces.Precision;
import proguard.analysis.cpa.interfaces.PrecisionAdjustment;

/**
 * This is the result of {@link PrecisionAdjustment}, namely an {@link AbstractState} and {@link Precision}.
 *
 * @author Dmitry Ivanov
 */
public class PrecisionAdjustmentResult
{
    private final AbstractState abstractState;
    private final Precision     precision;

    /**
     * Create a precision adjustment result tuple.
     *
     * @param abstractState abstract state
     * @param precision     precision
     */
    public PrecisionAdjustmentResult(AbstractState abstractState, Precision precision)
    {
        this.abstractState = abstractState;
        this.precision = precision;
    }

    /**
     * Returns the abstract state.
     */
    public AbstractState getAbstractState()
    {
        return abstractState;
    }

    /**
     * Returns the precision.
     */
    public Precision getPrecision()
    {
        return precision;
    }
}
