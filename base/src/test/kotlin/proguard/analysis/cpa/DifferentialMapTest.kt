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
import proguard.analysis.cpa.defaults.DifferentialMap

class DifferentialMapTest : FreeSpec({

    "Initialization with another map produces an equal result to the input" {
        val diffMap = DifferentialMap(mapOf(1 to 2))
        diffMap shouldBe mapOf(1 to 2)
    }

    "Initial node is the root" {
        val diffMap = DifferentialMap(mapOf(1 to 2))
        diffMap.depth shouldBe 0
    }

    "Insertion increases the depth and affects the result" {
        val diffMap = DifferentialMap<Int, Int>()
        diffMap[1] = 2
        diffMap.depth shouldBe 1
        diffMap shouldBe mapOf(1 to 2)
    }

    "Deletion increases the depth and affects the result" {
        val diffMap = DifferentialMap<Int, Int>()
        diffMap[1] = 2
        diffMap[2] = 3
        diffMap.remove(1)
        diffMap.depth shouldBe 3
        diffMap shouldBe mapOf(2 to 3)
    }

    "Collapse resets the depth while preserving the result" {
        val diffMap = DifferentialMap<Int, Int>()
        diffMap[1] = 2
        diffMap.collapse()
        diffMap.depth shouldBe 0
        diffMap shouldBe mapOf(1 to 2)
    }

    "Collapse criterion triggers on insertion" {
        val diffMap = DifferentialMap<Int, Int>(mapOf()) { m -> m.depth > 1 }
        diffMap[1] = 2
        diffMap[2] = 3
        diffMap.depth shouldBe 0
    }

    "Collapse criterion triggers on deletion" {
        val diffMap = DifferentialMap<Int, Int>(mapOf()) { m -> m.depth > 1 }
        diffMap.remove(1)
        diffMap.remove(2)
        diffMap.depth shouldBe 0
    }

    "Entry set propagates deletion" {
        val diffMap = DifferentialMap(mapOf(1 to 2, 2 to 3))
        diffMap.entries.removeIf { it.key == 1 }
        diffMap shouldBe mapOf(2 to 3)
    }

    "Entry set propagates clear" {
        val diffMap = DifferentialMap(mapOf(1 to 2))
        diffMap.entries.clear()
        diffMap shouldBe mapOf()
    }

    "Key set propagates deletion" {
        val diffMap = DifferentialMap(mapOf(1 to 2, 2 to 3))
        diffMap.keys.removeIf { it == 1 }
        diffMap shouldBe mapOf(2 to 3)
    }

    "Key set propagates clear" {
        val diffMap = DifferentialMap(mapOf(1 to 2))
        diffMap.keys.clear()
        diffMap shouldBe mapOf()
    }

    "Value set propagates deletion" {
        val diffMap = DifferentialMap(mapOf(1 to 2, 2 to 3))
        diffMap.values.removeIf { it == 2 }
        diffMap shouldBe mapOf(2 to 3)
    }

    "Value set propagates clear" {
        val diffMap = DifferentialMap(mapOf(1 to 2))
        diffMap.values.clear()
        diffMap shouldBe mapOf()
    }

    "Entries propagate modification" {
        val diffMap = DifferentialMap(mapOf(1 to 2))
        diffMap.entries.forEach { it.setValue(3) }
        diffMap shouldBe mapOf(1 to 3)
    }

    "Entry modification does not leak" {
        val map = mapOf(1 to 2)
        val diffMap = DifferentialMap(map)
        diffMap.entries.forEach { it.setValue(3) }
        map shouldBe mapOf(1 to 2)
    }
})
