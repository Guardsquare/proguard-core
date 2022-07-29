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

package proguard.testutils.cpa

import proguard.analysis.cpa.interfaces.AbstractState
import proguard.analysis.cpa.interfaces.Precision
import proguard.analysis.cpa.interfaces.TransferRelation

class BoundedAdditiveTransferRelation(
    private val step: Int,
    private val cap: Int
) : TransferRelation {
    override fun getAbstractSuccessors(
        abstractState: AbstractState?,
        precision: Precision?
    ): MutableCollection<out AbstractState> {
        if (abstractState is IntegerAbstractState) {
            return mutableListOf(IntegerAbstractState(minOf(abstractState.v + step, cap)))
        } else {
            throw IllegalArgumentException()
        }
    }
}
