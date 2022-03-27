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

package proguard.analysis.cpa.interfaces;

import java.util.Collection;

/**
 * The {@link StopOperator} decides if {@link Algorithm} should stop.
 *
 * @author Dmitry Ivanov
 */
public interface StopOperator
{

    /**
     * The operator may decide based on the (generalized under the given {@code precision}) convergence.
     * In this case it needs to look up the {@code abstractState} in the {@code reachedAbstractStates}.
     * Otherwise, it can return {@code true} if sufficient information is collected, e.g., a safety property is violated.
     */
    boolean stop(AbstractState abstractState, Collection<? extends AbstractState> reachedAbstractStates, Precision precision);
}
