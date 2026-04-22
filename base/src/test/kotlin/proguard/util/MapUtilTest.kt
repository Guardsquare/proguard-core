package proguard.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.util.TreeMap
import io.kotest.matchers.maps.shouldContainExactly as shouldContainExactlyMap

class MapUtilTest : BehaviorSpec({
    Given("key-value pairs") {
        When("MapUtil.of is called") {
            val map = MapUtil.of<String, Int>("hello", 1, "world", 2)

            Then("it should create the exact map") {
                map.shouldContainExactlyMap(mapOf("hello" to 1, "world" to 2))
            }
        }
    }

    Given("a TreeMap with paths") {
        val map = TreeMap<String, String>()
        map["a/a/a"] = "a/a/a"
        map["a/b/a"] = "a/b/a"
        map["a/b/b"] = "a/b/b"
        map["a/c/c"] = "a/c/c"

        And("a NameParser matcher for 'a/b/*'") {
            val matcher = NameParser().parse("a/b/*")

            When("filterTreeMap is executed") {
                val list = mutableListOf<String>()
                MapUtil.filterTreeMap(map, matcher, list::add)

                Then("it should only collect paths matching the pattern") {
                    list.shouldContainExactly("a/b/a", "a/b/b")
                }
            }
        }
    }

    Given("a TreeMap where the root node is in the middle of a prefix cluster") {
        val map = TreeMap<String, String>()

        // root
        map["mango"] = "mango"

        map["macaroni"] = "macaroni"
        map["map"] = "map"
        map["machine"] = "machine"
        map["market"] = "market"
        map["mad"] = "mad"
        map["maya"] = "maya"

        map["apple"] = "apple"
        map["zebra"] = "zebra"

        And("a StringMatcher that filters for the prefix 'ma' and ends with a vowel") {
            val matcher = object : StringMatcher() {
                override fun prefix(): String = "ma"

                override fun matches(string: String, beginOffset: Int, endOffset: Int): Boolean {
                    val sub = string.substring(beginOffset, endOffset)
                    val endsWithVowel = sub.matches(".*[aeiou]$".toRegex())
                    return sub.startsWith(prefix()) && endsWithVowel
                }
            }

            When("filterTreeMap is executed to find matching candidates") {
                val consumedValues = mutableListOf<String>()

                MapUtil.filterTreeMap(map, matcher) { value ->
                    consumedValues.add(value)
                }

                Then("it should correctly traverse past the root to find all matches in the prefix cluster") {
                    // words that start with "ma", end with vowel
                    consumedValues.shouldContainExactlyInAnyOrder(
                        "macaroni",
                        "machine",
                        "mango",
                        "maya",
                    )
                }
            }
        }
    }
})
