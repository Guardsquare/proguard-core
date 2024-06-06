package proguard.dexfile.reader

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import proguard.android.testutils.getDexFromJar

class DexContainerReaderTest : BehaviorSpec({
    Given("A v41 dex file") {
        When("we read it") {
            val dex = getDexFromJar("v41.jar")
            val reader = DexReaderFactory.createSingleReader(dex)
            Then("the reader is a DexContainerReader") {
                reader.shouldBeInstanceOf<DexContainerReader>()
            }
            Then("The version is correctly set") {
                reader.dexVersion shouldBe DexConstants.DEX_041
            }
            Then("the classes are all read correctly") {
                reader.classNames shouldContainExactly listOf("LMain;", "LSecond;")
            }
            Then("all strings are read correctly from all contained files and not duplicated") {
                val strings = mutableListOf<String>()
                reader.accept {
                    strings.add(it)
                }
                strings.size shouldBe strings.toSet().size
                strings shouldContain "Main.java"
                strings shouldContain "Second.java"
            }
        }
    }
})
