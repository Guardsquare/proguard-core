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
import proguard.analysis.cpa.defaults.ListAbstractState
import proguard.testutils.cpa.IntegerAbstractState

class ListAbstractStateTest : FreeSpec({

    val stateEmpty = ListAbstractState<IntegerAbstractState>()
    val stateShort1 = ListAbstractState<IntegerAbstractState>()
    val stateShort2 = ListAbstractState<IntegerAbstractState>()
    val stateShortSmall = ListAbstractState<IntegerAbstractState>()
    val stateLong = ListAbstractState<IntegerAbstractState>()

    stateShort1.add(IntegerAbstractState(3))
    stateShort1.add(IntegerAbstractState(2))
    stateShort1.add(IntegerAbstractState(1))

    stateShort2.add(IntegerAbstractState(3))
    stateShort2.add(IntegerAbstractState(4))
    stateShort2.add(IntegerAbstractState(-1))

    stateLong.add(IntegerAbstractState(3))
    stateLong.add(IntegerAbstractState(0))
    stateLong.add(IntegerAbstractState(5))
    stateLong.add(IntegerAbstractState(-2))
    stateLong.add(IntegerAbstractState(1))

    stateShortSmall.add(IntegerAbstractState(-5))
    stateShortSmall.add(IntegerAbstractState(-5))
    stateShortSmall.add(IntegerAbstractState(-5))

    "Empty list is the neutral element" {
        stateEmpty.join(stateShort1) shouldBe stateShort1
        stateShort1.join(stateEmpty) shouldBe stateShort1
    }

    "Lists of the same size are correctly joined" {
        val result = ListAbstractState<IntegerAbstractState>()
        result.add(IntegerAbstractState(3))
        result.add(IntegerAbstractState(4))
        result.add(IntegerAbstractState(1))

        stateShort1.join(stateShort2) shouldBe result
    }

    "Lists of the different sizes are correctly joined" {
        val result = ListAbstractState<IntegerAbstractState>()
        result.add(IntegerAbstractState(3))
        result.add(IntegerAbstractState(2))
        result.add(IntegerAbstractState(5))
        result.add(IntegerAbstractState(-2))
        result.add(IntegerAbstractState(1))

        stateShort1.join(stateLong) shouldBe result
        stateLong.join(stateShort1) shouldBe result
    }

    "Comparison is reflexive" {
        stateShort1.isLessOrEqual(stateShort1) shouldBe true
    }

    "Comparison is antisymmetric" {
        stateShortSmall.isLessOrEqual(stateShort1) shouldBe true
        stateShort1.isLessOrEqual(stateShortSmall) shouldBe false
    }

    "Empty list is the bottom" {
        stateEmpty.isLessOrEqual(stateLong) shouldBe true
        stateLong.isLessOrEqual(stateEmpty) shouldBe false
    }

    "List returns the default value when out of bound" {
        stateEmpty.getOrDefault(5, IntegerAbstractState(100)) shouldBe IntegerAbstractState(100)
    }

    "List extends on random access write" {
        val stateEmpty2 = stateEmpty.copy() as ListAbstractState<IntegerAbstractState>
        stateEmpty2.set(5, IntegerAbstractState(10), IntegerAbstractState(100))
        stateEmpty2[2] shouldBe IntegerAbstractState(100)
        stateEmpty2[5] shouldBe IntegerAbstractState(10)
    }
})
