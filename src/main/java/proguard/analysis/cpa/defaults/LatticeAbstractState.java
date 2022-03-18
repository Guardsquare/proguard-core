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

import proguard.analysis.cpa.interfaces.AbstractDomain;
import proguard.analysis.cpa.interfaces.AbstractState;

/**
 * The {@link LatticeAbstractState} is an {@link AbstractDomain}
 * with concrete interfaces.
 *
 * @author Dmitry Ivanov
 */
public interface LatticeAbstractState<AbstractStateT extends LatticeAbstractState<AbstractStateT>> extends AbstractState
{

    /**
     * Computes a join over itself and the {@code abstractState}.
     */
    AbstractStateT join(AbstractStateT abstractState);

    /**
     * Compares itself to the {@code abstractState}.
     */
    boolean isLessOrEqual(AbstractStateT abstractState);

    /**
     * Strictly compares itself to the {@code abstractState}.
     */
    default boolean isLess(AbstractStateT abstractStateT)
    {
        return isLessOrEqual(abstractStateT) && !equals(abstractStateT);
    }
}
