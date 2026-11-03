/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2026 Guardsquare NV
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
package proguard.io

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.longs.shouldBeAtLeast
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import proguard.exception.ErrorId
import proguard.exception.ProguardCoreException
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path

class JrtDataEntrySourceTest : FunSpec({
    context("an invalid JAVA_HOME") {
        val notJavaHome = tempdir().toPath()

        test("No source should be created for all modules") {
            JrtDataEntrySource.tryCreate(notJavaHome, null).shouldBeNull()
        }

        test("No source should be created for specific module") {
            JrtDataEntrySource.tryCreate(notJavaHome, "java.base").shouldBeNull()
        }
    }

    context("a valid JAVA_HOME") {
        // Simply use the JAVA_HOME of the currently running JDK; test setup below should therefore only
        // reference resources which are present in all JDK versions used to run this test
        val javaHome = Path.of(System.getProperty("java.home"))

        data class ExpectedDataEntry(private val path: String, private val name: String)

        class ExpectingDataEntryReader(
            expectedEntries: Set<ExpectedDataEntry>,
            private val unexpectedEntries: Set<ExpectedDataEntry>,
        ) : DataEntryReader {
            private val expectedEntries: MutableSet<ExpectedDataEntry> = LinkedHashSet(expectedEntries)

            fun countBytes(inputStream: InputStream): Long {
                var skippedTotal: Long = 0

                while (true) {
                    // Don't try `skip(Long.MAX_VALUE)`, depending on how simple the implementation of `skip`
                    // is, that might trigger an OutOfMemoryError or cause numeric overflow
                    val skipped = inputStream.skip(10_000)
                    // Check for broken `skip` returning negative values
                    skipped shouldBeAtLeast 0

                    if (skipped == 0L) {
                        // Check if end of stream was reached
                        if (inputStream.read() == -1) {
                            return skippedTotal
                        } else {
                            skippedTotal++
                        }
                    } else {
                        skippedTotal += skipped
                    }
                }
            }

            override fun read(dataEntry: DataEntry) {
                withClue("for entry '$dataEntry'") {
                    // Assertions which apply for all entries
                    dataEntry.getName() shouldBe dataEntry.getOriginalName()
                    dataEntry.isDirectory() shouldBe false
                    dataEntry.getParent().shouldBeNull()

                    val entry = ExpectedDataEntry(dataEntry.toString(), dataEntry.getName())
                    if (expectedEntries.remove(entry)) {
                        // Perform additional more expensive checks only for the specified expected entries
                        val inputStream = dataEntry.getInputStream()
                        val bytes = countBytes(inputStream)
                        dataEntry.closeInputStream()
                        bytes shouldBeGreaterThan 0
                        bytes shouldBe dataEntry.getSize()
                    }

                    unexpectedEntries shouldNotContain entry
                }
            }

            fun assertFoundAll() {
                expectedEntries.shouldBeEmpty()
            }
        }

        /** Entry in the `java.base` module  */
        val javaBaseEntry = ExpectedDataEntry(
            "jrt:/modules/java.base/java/lang/String.class",
            "java/lang/String.class",
        )

        /** Entry in the `java.compiler` module  */
        val javaCompilerEntry = ExpectedDataEntry(
            "jrt:/modules/java.compiler/javax/lang/model/SourceVersion.class",
            "javax/lang/model/SourceVersion.class",
        )

        test("It should read all modules") {
            val source = JrtDataEntrySource.tryCreate(javaHome, null)
            source.shouldNotBeNull()

            val entryReader =
                ExpectingDataEntryReader(
                    setOf(javaBaseEntry, javaCompilerEntry),
                    emptySet(),
                )
            source.pumpDataEntries(entryReader)
            entryReader.assertFoundAll()
        }

        test("It should read java.base module") {
            val source = JrtDataEntrySource.tryCreate(javaHome, "java.base")
            source.shouldNotBeNull()

            // Should only find classes from `java.base`
            val entryReader =
                ExpectingDataEntryReader(
                    setOf(javaBaseEntry),
                    setOf(javaCompilerEntry),
                )
            source.pumpDataEntries(entryReader)
            entryReader.assertFoundAll()
        }

        test("It should read java.compiler module") {
            val source = JrtDataEntrySource.tryCreate(javaHome, "java.compiler")
            source.shouldNotBeNull()

            // Should only find classes from `java.compiler`
            val entryReader =
                ExpectingDataEntryReader(
                    setOf(javaCompilerEntry),
                    setOf(javaBaseEntry),
                )
            source.pumpDataEntries(entryReader)
            entryReader.assertFoundAll()
        }

        test("Reading a non-existing module") {
            val source = JrtDataEntrySource.tryCreate(javaHome, "java.foo")
            source.shouldNotBeNull()
            val reader = DataEntryReader { throw AssertionError("should not be called") }
            val exc = shouldThrow<ProguardCoreException> { source.pumpDataEntries(reader) }
            exc.componentErrorId shouldBe ErrorId.JRT_INVALID_MODULE
            exc.message shouldBe "Module java.foo does not exist for JDK at path $javaHome"
        }

        test("Malformed jrt url") {
            mockkConstructor(URI::class) {
                val urlException = MalformedURLException()
                every { anyConstructed<URI>().toURL() } throws urlException
                val source = JrtDataEntrySource.tryCreate(javaHome, "java.base")
                source.shouldNotBeNull()
                val reader = DataEntryReader { throw AssertionError("should not be called") }
                val exc = shouldThrow<ProguardCoreException> { source.pumpDataEntries(reader) }
                exc.componentErrorId shouldBe ErrorId.JRT_INVALID_URL
                exc.cause shouldBe urlException
            }
        }

        test("JRT File system exception") {
            mockkStatic(FileSystems::class) {
                val ioException = IOException()
                every { FileSystems.newFileSystem(any<URI>(), any(), any()) } throws ioException
                val source = JrtDataEntrySource.tryCreate(javaHome, "java.base")
                source.shouldNotBeNull()
                val reader = DataEntryReader { throw AssertionError("should not be called") }
                val exc = shouldThrow<ProguardCoreException> { source.pumpDataEntries(reader) }
                exc.componentErrorId shouldBe ErrorId.JRT_FILESYSTEM_ERROR
                exc.cause shouldBe ioException
            }
        }
    }
})
