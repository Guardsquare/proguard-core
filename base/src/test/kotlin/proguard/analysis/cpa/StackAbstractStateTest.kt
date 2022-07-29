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
import proguard.analysis.cpa.defaults.StackAbstractState
import proguard.testutils.cpa.IntegerAbstractState

class StackAbstractStateTest : FreeSpec({

    val stateEmpty = StackAbstractState<IntegerAbstractState>()
    val stateShort1 = StackAbstractState<IntegerAbstractState>()
    val stateShort2 = StackAbstractState<IntegerAbstractState>()
    val stateShortSmall = StackAbstractState<IntegerAbstractState>()
    val stateLong = StackAbstractState<IntegerAbstractState>()

    stateShort1.push(IntegerAbstractState(3))
    stateShort1.push(IntegerAbstractState(2))
    stateShort1.push(IntegerAbstractState(1))

    stateShort2.push(IntegerAbstractState(3))
    stateShort2.push(IntegerAbstractState(4))
    stateShort2.push(IntegerAbstractState(-1))

    stateLong.push(IntegerAbstractState(5))
    stateLong.push(IntegerAbstractState(4))
    stateLong.push(IntegerAbstractState(5))
    stateLong.push(IntegerAbstractState(-2))
    stateLong.push(IntegerAbstractState(1))

    stateShortSmall.push(IntegerAbstractState(-5))
    stateShortSmall.push(IntegerAbstractState(-5))
    stateShortSmall.push(IntegerAbstractState(-5))

    "Empty stack is the neutral element" {
        stateEmpty.join(stateShort1) shouldBe stateShort1
        stateShort1.join(stateEmpty) shouldBe stateShort1
    }

    "Stacks of the same size are correctly joined" {
        val result = StackAbstractState<IntegerAbstractState>()
        result.push(IntegerAbstractState(3))
        result.push(IntegerAbstractState(4))
        result.push(IntegerAbstractState(1))

        stateShort1.join(stateShort2) shouldBe result
    }

    "Stacks of the different sizes are correctly joined" {
        val result = StackAbstractState<IntegerAbstractState>()
        result.push(IntegerAbstractState(5))
        result.push(IntegerAbstractState(4))
        result.push(IntegerAbstractState(5))
        result.push(IntegerAbstractState(2))
        result.push(IntegerAbstractState(1))

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

    "Empty stack is the bottom" {
        stateEmpty.isLessOrEqual(stateLong) shouldBe true
        stateLong.isLessOrEqual(stateEmpty) shouldBe false
    }
})
