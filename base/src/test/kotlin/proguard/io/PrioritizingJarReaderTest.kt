package proguard.io

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream

class PrioritizingJarReaderTest : BehaviorSpec({

    // Create and return a temporary zip file containing entries with the given names.
    fun createZip(entryNames: List<String>): File {
        val tempFile = Files.createTempFile("entries", ".zip")
        ZipOutputStream(tempFile.outputStream()).use { zip ->
            for (entryName in entryNames) {
                zip.putNextEntry(ZipEntry(entryName))
            }
        }
        return tempFile.toFile().also { it.deleteOnExit() }
    }

    // Utility function that returns a list of entry names in the order they were read.
    fun prioritizedRead(order: Map<String, Int>, dataEntry: DataEntry): ArrayList<String> {
        val readEntries = arrayListOf<String>()
        val loggingReader: (DataEntry) -> Unit = { dataEntry -> readEntries.add(dataEntry.name) }
        val prioritizingReader = PrioritizingJarReader(order, loggingReader)
        prioritizingReader.read(dataEntry)
        return readEntries
    }

    Given("A data entry representing a file") {
        // The default order of reading the entries in the zip file.
        val defaultOrder = arrayListOf("a", "b", "dir/a", "dir/b", "z")
        val dataEntry = FileDataEntry(createZip(defaultOrder))

        When("The data entry is read and no priorities are specified") {
            val order = prioritizedRead(emptyMap(), dataEntry)

            Then("The entries should be read in their original order") {
                order.shouldBe(defaultOrder)
            }
        }

        When("The data entry is read and one of the entries has a higher priority") {
            val priorities = mapOf("dir/b" to -1)
            val order = prioritizedRead(priorities, dataEntry)

            Then("The prioritized entry should be read first") {
                val expectedOrder = arrayListOf("dir/b", "a", "b", "dir/a", "z")
                order.shouldBe(expectedOrder)
            }
        }

        When("The data entry is read and a set of entries matching a filter has a higher priority") {
            val priorities = mapOf("dir/*" to -1)
            val order = prioritizedRead(priorities, dataEntry)

            Then("The prioritized entries should be read first") {
                val expectedOrder = arrayListOf("dir/a", "dir/b", "a", "b", "z")
                order.shouldBe(expectedOrder)
            }
        }

        When("The data entry is read and one of the entries has a lower priority") {
            val priorities = mapOf("a" to 1)
            val order = prioritizedRead(priorities, dataEntry)

            Then("The deprioritized entry should be read last") {
                val expectedOrder = arrayListOf("b", "dir/a", "dir/b", "z", "a")
                order.shouldBe(expectedOrder)
            }
        }

        When("The data entry is read and a set of entries matching a filter has a lower priority") {
            val priorities = mapOf("dir/*" to 1)
            val order = prioritizedRead(priorities, dataEntry)

            Then("The deprioritized entries should be read last") {
                val expectedOrder = arrayListOf("a", "b", "z", "dir/a", "dir/b")
                order.shouldBe(expectedOrder)
            }
        }

        When("The data entry is read and one of the entries matches multiple filters with different priorities") {
            val priorities = mapOf("dir**a" to -1, "*/a" to 1, "dir/a" to 2)
            val order = prioritizedRead(priorities, dataEntry)

            Then("The lowest matching priority should be taken into account") {
                val expectedOrder = arrayListOf("dir/a", "a", "b", "dir/b", "z")
                order.shouldBe(expectedOrder)
            }
        }

        When("The data entry is read and all entries are assigned unique priorities") {
            val priorities = mapOf("a" to 1, "b" to 2, "dir/" to 3, "dir/a" to 4, "dir/b" to 5, "z" to 6)
            val order = prioritizedRead(priorities, dataEntry)

            Then("Entries with lower priority should be read first") {
                val expectedOrder = arrayListOf("a", "b", "dir/a", "dir/b", "z")
                order.shouldBe(expectedOrder)
            }
        }

        When("The data entry is read and all entries have the same priority") {
            val priorities = mapOf("**" to 3)
            val order = prioritizedRead(priorities, dataEntry)

            Then("The entries should be read in their original order") {
                order.shouldBe(defaultOrder)
            }
        }
    }
})
