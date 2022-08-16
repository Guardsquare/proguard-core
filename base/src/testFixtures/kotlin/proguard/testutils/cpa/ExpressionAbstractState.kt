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

class ExpressionAbstractState(
    var values: Set<JvmExpression>
) : LatticeAbstractState<ExpressionAbstractState> {

    override fun join(abstractState: ExpressionAbstractState?): ExpressionAbstractState {
        if (abstractState == null) {
            return this
        }
        val result = HashSet<JvmExpression>()
        result.addAll(values)
        result.addAll(abstractState.values)
        return if (values.containsAll(result))
            this
        else if (abstractState.values.containsAll(result))
            abstractState
        else
            ExpressionAbstractState(result)
    }

    override fun isLessOrEqual(abstractState: ExpressionAbstractState?): Boolean {
        return abstractState is ExpressionAbstractState && abstractState.values.containsAll(values)
    }

    override fun equals(other: Any?): Boolean {
        return other is ExpressionAbstractState && values == other.values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }

    override fun copy(): ExpressionAbstractState {
        return ExpressionAbstractState(values)
    }
}
