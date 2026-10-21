package proguard.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.kotest.property.resolution.default

class SimpleProcessableTest : FunSpec({
    val allFlagInts = (1..31).scan(1) { acc, _ -> acc * 2 }
    val allFlags = Arb.of(allFlagInts)
    val flagLists = Arb.list(allFlags, range = 1..32)
        .map { flagList -> flagList.distinct() }

    test("Adding a flag should result in that flag being set") {
        checkAll(Arb.default<Int>(), allFlags) { flagSet, toSetFlag ->
            val processable = SimpleProcessable(flagSet, null)
            processable.addProcessingFlags(toSetFlag)
            allFlagInts.forEach { flag ->
                if (toSetFlag == flag) {
                    // This is the flag that should now be set.
                    processable.hasProcessingFlags(flag) shouldBe true
                } else {
                    // Other flags should stay untouched.
                    processable.hasProcessingFlags(flag) shouldBe ((flagSet.inv() and flag) == 0)
                }
            }
        }
    }

    test("Removing a flag should result in that flag not being set") {
        checkAll(Arb.default<Int>(), allFlags) { flagSet, toRemoveFlag ->
            val processable = SimpleProcessable(flagSet, null)
            processable.removeProcessingFlags(toRemoveFlag)
            allFlagInts.forEach { flag ->
                if (toRemoveFlag == flag) {
                    // This is the flag that should now not be set.
                    processable.hasProcessingFlags(flag) shouldBe false
                } else {
                    // Other flags should stay untouched.
                    processable.hasProcessingFlags(flag) shouldBe ((flagSet.inv() and flag) == 0)
                }
            }
        }
    }

    test("Adding a combined set of flags should result in all flags being set") {
        checkAll(Arb.default<Int>(), flagLists) { flagSet, toSetFlags ->
            val processable = SimpleProcessable(flagSet, null)
            processable.addProcessingFlags(*toSetFlags.toIntArray())
            allFlagInts.forEach { flag ->
                if (toSetFlags.contains(flag)) {
                    // This is a flag that should now be set.
                    processable.hasProcessingFlags(flag) shouldBe true
                } else {
                    // Other flags should stay untouched.
                    processable.hasProcessingFlags(flag) shouldBe ((flagSet.inv() and flag) == 0)
                }
            }
        }
    }

    test("Removing a combined set of flags should result in all flags not being set") {
        checkAll(Arb.default<Int>(), flagLists) { flagSet, toRemoveFlags ->
            val processable = SimpleProcessable(flagSet, null)
            processable.removeProcessingFlags(*toRemoveFlags.toIntArray())
            allFlagInts.forEach { flag ->
                if (toRemoveFlags.contains(flag)) {
                    // This is a flag that should now be removed.
                    processable.hasProcessingFlags(flag) shouldBe false
                } else {
                    // Other flags should stay untouched.
                    processable.hasProcessingFlags(flag) shouldBe ((flagSet.inv() and flag) == 0)
                }
            }
        }
    }
})
