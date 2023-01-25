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

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import proguard.analysis.cpa.defaults.LimitedHashMap
import java.util.Optional

class LimitedHashMapTest : FreeSpec({

    "Initialization is not affected by the limit" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map shouldBe mapOf(1 to 2)
    }

    "Put is affected by the limit" {
        val map = LimitedHashMap<Int, Int> { _, k, _ -> Optional.of(k) }
        map[1] = 2
        map shouldBe mapOf()
    }

    "PutAll is affected by the limit" {
        val map = LimitedHashMap<Int, Int> { _, k, _ -> Optional.of(k) }
        map.putAll(mapOf(1 to 2))
        map shouldBe mapOf()
    }

    "Map can be used for defining the limit" {
        val map = LimitedHashMap<Int, Int> { m, k, _ -> if (m.size > 1) Optional.of(k) else Optional.empty() }
        map[1] = 2
        map[2] = 3
        map shouldBe mapOf(1 to 2)
    }

    "Key can be used for defining the limit" {
        val map = LimitedHashMap<Int, Int> { _, k, _ -> if (k < 0) Optional.of(k) else Optional.empty() }
        map[1] = 2
        map[-2] = 3
        map shouldBe mapOf(1 to 2)
    }

    "Value can be used for defining the limit" {
        val map = LimitedHashMap<Int, Int> { _, k, v -> if (v < 0) Optional.of(k) else Optional.empty() }
        map[1] = 2
        map[2] = -3
        map shouldBe mapOf(1 to 2)
    }

    "putIfAbsent respects the removal selector" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map.putIfAbsent(3, 4)
        map shouldBe mapOf(1 to 2)
    }

    "putIfAbsent preserves existing mappings" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map.putIfAbsent(1, 3)
        map shouldBe mapOf(1 to 2)
    }

    "putIfAbsent does not leak null values" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, _, v -> if (v == null) throw NullPointerException() else Optional.empty() }
        shouldNotThrowAny {
            map.putIfAbsent(2, null)
        }
    }

    "computeIfAbsent respects the removal selector" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map.computeIfAbsent(3) { _ -> 4 }
        map shouldBe mapOf(1 to 2)
    }

    "computeIfAbsent does not leak null values" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, _, v -> if (v == null) throw NullPointerException() else Optional.empty() }
        shouldNotThrowAny {
            map.computeIfAbsent(3) { _ -> null }
        }
    }

    "computeIfAbsent preserves existing mappings" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map.computeIfAbsent(1) { _ -> 3 }
        map shouldBe mapOf(1 to 2)
    }

    "compute respects the removal selector" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map.compute(1) { _, _ -> 3 }
        map shouldBe mapOf()
    }

    "compute does not leak null values" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, _, v -> if (v == null) throw NullPointerException() else Optional.empty() }
        shouldNotThrowAny {
            map.compute(1) { _, _ -> null }
        }
    }

    "merge respects the removal selector" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, k, _ -> Optional.of(k) }
        map.merge(1, 3) { _, _ -> 3 }
        map shouldBe mapOf()
    }

    "merge does not leak null values" {
        val map = LimitedHashMap(mapOf(1 to 2)) { _, _, v -> if (v == null) throw NullPointerException() else Optional.empty() }
        shouldNotThrowAny {
            map.merge(1, 3) { _, _ -> null }
        }
    }
})
