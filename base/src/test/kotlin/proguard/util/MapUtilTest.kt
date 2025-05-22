package proguard.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import java.util.TreeMap

class MapUtilTest : FunSpec({
    test("MapUtil.of") {
        val map = MapUtil.of<String, Integer>("hello", 1, "world", 2)
        map shouldContainExactly mapOf("hello" to 1, "world" to 2)
    }

    test("MapUtil.filterTreeMap") {
        val map = TreeMap<String, String>()
        map.put("a/a/a", "a/a/a")
        map.put("a/b/a", "a/b/a")
        map.put("a/b/b", "a/b/b")
        map.put("a/c/c", "a/c/c")
        val list = mutableListOf<String>()
        MapUtil.filterTreeMap(map, NameParser().parse("a/b/*"), list::add)
        list shouldContainExactly listOf("a/b/a", "a/b/b")
    }
})
