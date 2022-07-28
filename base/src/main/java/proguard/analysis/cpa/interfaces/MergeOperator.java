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

/**
 * The {@link MergeOperator} defines how (and whether) the older {@link AbstractState} should be updated with the newly discovered {@link AbstractState}.
 *
 * @author Dmitry Ivanov
 */
public interface MergeOperator
{

    /**
     * The operator uses the {@code abstractState1} to weaken {@code abstractState2} depending on {@code precision}.
     * Thus, it is asymmetric regarding its first two parameters. E.g., return {@code abstractState2} for no merging.
     * To guarantee the correct behavior of the algorithm implementations must have no side effects.
     */
    AbstractState merge(AbstractState abstractState1, AbstractState abstractState2, Precision precision);
}
