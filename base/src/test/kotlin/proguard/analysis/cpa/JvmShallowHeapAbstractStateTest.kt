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
import proguard.analysis.cpa.defaults.HashMapAbstractState
import proguard.analysis.cpa.jvm.cfa.nodes.JvmUnknownCfaNode
import proguard.analysis.cpa.jvm.state.heap.tree.JvmShallowHeapAbstractState
import proguard.testutils.cpa.IntegerAbstractState

class JvmShallowHeapAbstractStateTest : FreeSpec({

    val defaultValue = IntegerAbstractState(0)
    val referenceClass = Integer::class.java

    "Array creation returns the default value" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.newArray("", listOf(), JvmUnknownCfaNode.INSTANCE) shouldBe defaultValue
    }

    "Object creation returns the default value" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.newObject("", JvmUnknownCfaNode.INSTANCE) shouldBe defaultValue
    }

    "Empty field can be stored" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.setField(1, "", IntegerAbstractState(2))
        state.getFieldOrDefault(1, "", defaultValue) shouldBe IntegerAbstractState(2)
    }

    "Nonempty fields are ignored" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.setField(1, "field", IntegerAbstractState(2))
        state.getFieldOrDefault(1, "field", defaultValue) shouldBe defaultValue
    }

    "Arrays are treated as singletons" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.setArrayElement(1, IntegerAbstractState(2), IntegerAbstractState(3))
        state.setArrayElement(1, IntegerAbstractState(2), IntegerAbstractState(4))
        state.getArrayElementOrDefault(1, IntegerAbstractState(2), defaultValue) shouldBe IntegerAbstractState(4)
    }

    "Join contains elements of its both operands" {
        val state1 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(2))), referenceClass, defaultValue)
        val state2 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(2) to IntegerAbstractState(3))), referenceClass, defaultValue)
        val result = JvmShallowHeapAbstractState(
            HashMapAbstractState(
                mutableMapOf(Integer(1) to IntegerAbstractState(2), Integer(2) to IntegerAbstractState(3))
            ),
            referenceClass,
            defaultValue
        )
        state1.join(state2) shouldBe result
    }

    "Join is applied to values with the same key" {
        val state1 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(2))), referenceClass, defaultValue)
        val state2 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(3))), referenceClass, defaultValue)
        val result = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(3))), referenceClass, defaultValue)
        state1.join(state2) shouldBe result
    }

    "Elements with incomparable key sets are incomparable" {
        val state1 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(2))), referenceClass, defaultValue)
        val state2 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(2) to IntegerAbstractState(3))), referenceClass, defaultValue)
        state1.isLessOrEqual(state2) shouldBe false
        state2.isLessOrEqual(state1) shouldBe false
    }

    "Comparison is pointwise for the same key sets" {
        val state1 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(2))), referenceClass, defaultValue)
        val state2 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(3))), referenceClass, defaultValue)
        state1.isLessOrEqual(state2) shouldBe true
        state2.isLessOrEqual(state1) shouldBe false
    }

    "If key sets are comparable, the comparison is pointwise on the smallest domain" {
        val state1 = JvmShallowHeapAbstractState(HashMapAbstractState(mutableMapOf(Integer(1) to IntegerAbstractState(2))), referenceClass, defaultValue)
        val state2 = JvmShallowHeapAbstractState(
            HashMapAbstractState(
                mutableMapOf(Integer(1) to IntegerAbstractState(2), Integer(2) to IntegerAbstractState(1))
            ),
            referenceClass,
            defaultValue
        )
        state1.isLessOrEqual(state2) shouldBe true
        state2.isLessOrEqual(state1) shouldBe false
    }

    "Pointwise incomparable value sets are incomparable" {
        val state1 = JvmShallowHeapAbstractState(
            HashMapAbstractState(
                mutableMapOf(Integer(1) to IntegerAbstractState(2), Integer(2) to IntegerAbstractState(1))
            ),
            referenceClass,
            defaultValue
        )
        val state2 = JvmShallowHeapAbstractState(
            HashMapAbstractState(
                mutableMapOf(Integer(1) to IntegerAbstractState(1), Integer(2) to IntegerAbstractState(2))
            ),
            referenceClass,
            defaultValue
        )
        state1.isLessOrEqual(state2) shouldBe false
        state2.isLessOrEqual(state1) shouldBe false
    }

    "Wrong array type yields the default value" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.getArrayElementOrDefault(1.0, IntegerAbstractState(1), defaultValue)shouldBe defaultValue
    }

    "Wrong object type yields the default value" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.getFieldOrDefault(1.0, "", defaultValue)shouldBe defaultValue
    }

    "Wrong array type is ignored" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.setArrayElement(1.0, IntegerAbstractState(1), defaultValue)
        state shouldBe JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
    }

    "Wrong object type is ignored" {
        val state = JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
        state.setField(1.0, "", defaultValue)
        state shouldBe JvmShallowHeapAbstractState(HashMapAbstractState(), referenceClass, defaultValue)
    }
})
