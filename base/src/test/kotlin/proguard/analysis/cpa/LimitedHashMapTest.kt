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
import proguard.analysis.cpa.defaults.LimitedHashMap

class LimitedHashMapTest : FreeSpec({

    "Initialization is not affected by the limit" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, _, _ -> true }
        map shouldBe mapOf(1 to 2)
    }

    "Put is affected by the limit" {
        val map = LimitedHashMap<Int, Int> { _, _, _ -> true }
        map[1] = 2
        map shouldBe mapOf()
    }

    "PutAll is affected by the limit" {
        val map = LimitedHashMap<Int, Int> { _, _, _ -> true }
        map.putAll(mapOf(1 to 2))
        map shouldBe mapOf()
    }

    "Map can be used for defining the limit" {
        val map = LimitedHashMap<Int, Int> { m, _, _ -> m.size > 0 }
        map[1] = 2
        map[2] = 3
        map shouldBe mapOf(1 to 2)
    }

    "Key can be used for defining the limit" {
        val map = LimitedHashMap<Int, Int> { _, k, _ -> k < 0 }
        map[1] = 2
        map[-2] = 3
        map shouldBe mapOf(1 to 2)
    }

    "Value can be used for defining the limit" {
        val map = LimitedHashMap<Int, Int> { _, _, v -> v < 0 }
        map[1] = 2
        map[2] = -3
        map shouldBe mapOf(1 to 2)
    }
})
