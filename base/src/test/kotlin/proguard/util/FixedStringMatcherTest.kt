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

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class FixedStringMatcherTest : FreeSpec({

    "Given a fixed string" - {
        val string = "foo"

        "When matched against itself" - {
            "Then they should match" {
                FixedStringMatcher(string).matches(string) shouldBe true
            }
        }

        "When matched against a longer string" - {
            val longerString = "foobar"

            "Then they should not match" {
                FixedStringMatcher(string).matches(longerString) shouldBe false
            }
        }

        "When matched against a shorter string" - {
            val shorterString = "fo"

            "Then they should not match" {
                FixedStringMatcher(string).matches(shorterString) shouldBe false
            }
        }

        "When the prefix is queried" - {
            "Then the prefix should be the same string" {
                FixedStringMatcher(string).prefix() shouldBe string
            }
        }
    }

    "Given a fixed string followed by another fixed string" - {
        val firstString = "foo"
        val secondString = "bar"

        val fixedStringMatcher = FixedStringMatcher(firstString, FixedStringMatcher(secondString))

        "When matched against the full string" - {
            val fullString = "foobar"

            "Then they should match" {
                fixedStringMatcher.matches(fullString) shouldBe true
            }

            "Then the prefix should match" {
                fixedStringMatcher.prefix() shouldBe fullString
            }
        }

        "When matched against just the matched string" - {
            "Then they should not match" {
                fixedStringMatcher.matches(firstString) shouldBe false
            }
        }

        "When the prefix is queried" - {
            "Then the prefix should be the full string" {
                fixedStringMatcher.prefix() shouldBe "foobar"
            }
        }
    }
})
