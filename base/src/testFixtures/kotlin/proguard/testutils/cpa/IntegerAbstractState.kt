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

import proguard.analysis.cpa.defaults.LatticeAbstractState
import kotlin.math.max

class IntegerAbstractState(val v: Int) : LatticeAbstractState<IntegerAbstractState> {
    override fun join(abstractState: IntegerAbstractState?): IntegerAbstractState {
        return if (abstractState != null) IntegerAbstractState(max(v, abstractState.v)) else this
    }

    override fun isLessOrEqual(abstractState: IntegerAbstractState?): Boolean {
        return if (abstractState != null) v <= abstractState.v else false
    }

    override fun equals(other: Any?): Boolean {
        return if (other is IntegerAbstractState) v == other.v else false
    }

    override fun toString(): String {
        return v.toString()
    }

    override fun hashCode(): Int {
        return v
    }

    override fun copy(): IntegerAbstractState {
        return IntegerAbstractState(v)
    }
}
