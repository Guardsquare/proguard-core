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

package proguard.analysis.cpa

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.SetAbstractState
import proguard.testutils.cpa.IntegerAbstractState

class SetAbstractStateTest : FreeSpec({

    val stateEmpty = SetAbstractState<IntegerAbstractState>()
    val state1 = SetAbstractState<IntegerAbstractState>()
    val state2 = SetAbstractState<IntegerAbstractState>()
    val state3 = SetAbstractState<IntegerAbstractState>()

    state1.add(IntegerAbstractState(1))

    state2.add(IntegerAbstractState(2))

    state3.add(IntegerAbstractState(1))
    state3.add(IntegerAbstractState(2))

    "Empty set is the neutral element" {
        stateEmpty.join(state1) shouldBe state1
        state1.join(stateEmpty) shouldBe state1
    }

    "Arbitrary sets are correctly joined" {
        state1.join(state2) shouldBe state3
    }

    "Comparison is reflexive" {
        state1.isLessOrEqual(state1) shouldBe true
    }

    "Comparison is antisymmetric" {
        state1.isLessOrEqual(state3) shouldBe true
        state3.isLessOrEqual(state1) shouldBe false
    }

    "Empty set is the bottom" {
        stateEmpty.isLessOrEqual(state1) shouldBe true
        state1.isLessOrEqual(stateEmpty) shouldBe false
    }

    "Sets which are not subsets of each other are not comparable" {
        state1.isLessOrEqual(state2) shouldBe false
        state2.isLessOrEqual(state1) shouldBe false
    }
})
