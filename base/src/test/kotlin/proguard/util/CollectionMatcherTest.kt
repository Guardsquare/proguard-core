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

class CollectionMatcherTest : FreeSpec({

    "Given a comma separated string" - {
        val string = """foo,'double,test',end"""

        val collectionParser = CollectionParser()

        "When the CollectionParser parses the string" - {
            val collectionMatcher = collectionParser.parse(string)

            "Then the obtained CollectionMatcher should contain a collection with all sub strings" {
                collectionMatcher.matches("foo") shouldBe true
                collectionMatcher.matches("double,test") shouldBe true
                collectionMatcher.matches("end") shouldBe true
                collectionMatcher.matches("nope") shouldBe false
                collectionMatcher.matches("double") shouldBe false
                collectionMatcher.matches("test") shouldBe false
            }
        }
    }
})
