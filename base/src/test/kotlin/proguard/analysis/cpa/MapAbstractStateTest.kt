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
import proguard.analysis.cpa.defaults.DifferentialMapAbstractState
import proguard.analysis.cpa.defaults.HashMapAbstractState
import proguard.analysis.cpa.defaults.LimitedHashMapAbstractState
import proguard.testutils.cpa.IntegerAbstractState
import java.util.Optional

class MapAbstractStateTest : FreeSpec({

    listOf(
        { HashMapAbstractState<Int, IntegerAbstractState>() },
        { DifferentialMapAbstractState() },
        { LimitedHashMapAbstractState { _, _, _ -> Optional.empty() } }
    ).forEach { supplier ->

        val stateEmpty = supplier.invoke()
        val state1 = supplier.invoke()
        val state2 = supplier.invoke()
        val state3 = supplier.invoke()
        val stateLarge = supplier.invoke()

        state1[1] = IntegerAbstractState(1)
        state1[2] = IntegerAbstractState(2)

        state2[2] = IntegerAbstractState(5)
        state2[3] = IntegerAbstractState(3)

        state3[1] = IntegerAbstractState(1)
        state3[2] = IntegerAbstractState(1)

        stateLarge[1] = IntegerAbstractState(5)
        stateLarge[2] = IntegerAbstractState(5)
        stateLarge[3] = IntegerAbstractState(5)

        "Empty map is the neutral element" {
            stateEmpty.join(state1) shouldBe state1
            state1.join(stateEmpty) shouldBe state1
        }

        "Arbitrary maps are correctly joined" {
            state1.join(state2) shouldBe mapOf(
                1 to IntegerAbstractState(1),
                2 to IntegerAbstractState(5),
                3 to IntegerAbstractState(3)
            )
        }

        "Comparison is reflexive" {
            state1.isLessOrEqual(state1) shouldBe true
        }

        "Comparison is antisymmetric" {
            state1.isLessOrEqual(stateLarge) shouldBe true
            stateLarge.isLessOrEqual(state1) shouldBe false
        }

        "Empty map is the bottom" {
            stateEmpty.isLessOrEqual(state1) shouldBe true
            state1.isLessOrEqual(stateEmpty) shouldBe false
        }

        "Comparison of maps with the same key sets is pointwise" {
            state1.isLessOrEqual(state3) shouldBe false
            state3.isLessOrEqual(state1) shouldBe true
        }
    }
})
