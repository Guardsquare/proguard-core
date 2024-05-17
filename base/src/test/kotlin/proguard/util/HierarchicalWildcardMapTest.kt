package proguard.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

class HierarchicalWildcardMapTest : FunSpec({

    data class Key(val a: String?, val b: String?, val c: String?)

    test("Test HierarchicalWildcardMap") {

        val wildMap = HierarchicalWildcardMap<Key, String, String>(3, { key -> arrayOf(key.a, key.b, key.c) }, null)

        // fill the table
        for (a in listOf("a", "b")) {
            for (b in listOf("a", "b", null)) {
                for (c in listOf("a", "b", null)) {
                    wildMap.put(Key(a, b, c), "$a.$b.$c")
                }
            }
        }

        wildMap.get(Key("a", "d", "d")) shouldBe "a.null.null"
        wildMap.get(Key("a", "a", "d")) shouldBe "a.a.null"
        wildMap.get(Key("a", "a", "b")) shouldBe "a.a.b"
        wildMap.get(Key("a", "a", "a")) shouldBe "a.a.a"
        wildMap.get(Key("dummy", "a", "a")) shouldBe null
        wildMap.get(Key("dummy", null, null)) shouldBe null
        wildMap.get(Key("a", null, null)) shouldBe "a.null.null"
        wildMap.get(Key(null, "a", "a")) shouldBeIn arrayOf("a.a.a", "b.a.a")
        wildMap.get(Key(null, null, "a")) shouldBeIn arrayOf("a.null.a", "b.null.a")
    }
})
