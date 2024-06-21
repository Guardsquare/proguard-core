/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

package proguard.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import proguard.exception.ErrorId
import proguard.exception.ProguardCoreException

class WildCardManagerTest : BehaviorSpec({
    Given("A WildcardManager without matches") {
        val wildcardManager = WildcardManager()
        When("a wildcard is retrieved") {
            Then("an exception is thrown") {
                val e = shouldThrow<ProguardCoreException> {
                    wildcardManager.wildCardIndex("<2>", 0)
                }
                e.componentErrorId shouldBe ErrorId.WILDCARD_WRONG_INDEX
            }
        }
    }
    Given("A WildCardManager with 8 matches") {
        val wildcardManager = WildcardManager()
        for (i in 1..8) {
            wildcardManager.createVariableStringMatcher(
                charArrayOf('a'),
                charArrayOf('b'),
                1,
                2,
                null,
            )
        }
        When("foo<1> is retrieved at index 0") {
            Then("the result should be -1")
            wildcardManager.wildCardIndex("foo<1>", 0) shouldBe -1
        }
        When("foo<9> is retreived at index 3") {
            Then("an exception is thrown") {
                val e = shouldThrow<ProguardCoreException> {
                    wildcardManager.wildCardIndex("foo<9>", 3)
                }
                e.componentErrorId shouldBe ErrorId.WILDCARD_WRONG_INDEX
            }
        }
        When("foo<1> is retrieved at index 3") {
            Then("the result should be 0")
            wildcardManager.wildCardIndex("foo<1>", 3) shouldBe 0
        }
        When("foo<8> is retrieved at index 3") {
            Then("the result should be 7")
            wildcardManager.wildCardIndex("foo<8>", 3) shouldBe 7
        }
    }
})
