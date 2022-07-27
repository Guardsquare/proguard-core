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

package proguard.analysis.cpa.bam;

import proguard.analysis.cpa.interfaces.AbstractState;

/**
 * This operator is used to avoid collision of program identifiers when returning from a procedure call. This operator does not compute any abstraction, but just performs simple operations as renaming
 * variables, depending on the domain.
 *
 * @author Carlo Alberto Pozzoli
 */
public interface RebuildOperator
{

    /**
     * Performs the rebuilding of the return state.
     *
     * @param predecessorCallState the state of the caller at the moment of the procedure call
     * @param expandedOutputState  the output of {@link ExpandOperator}
     * @return The state of the caller after the procedure call
     */
    AbstractState rebuild(AbstractState predecessorCallState, AbstractState expandedOutputState);
}
