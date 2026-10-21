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
package proguard.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JrtDataEntrySourceTest {
  @Test
  void testInvalidJavaHome(@TempDir Path notJavaHome) {
    assertNull(JrtDataEntrySource.tryCreate(notJavaHome, null));
    assertNull(JrtDataEntrySource.tryCreate(notJavaHome, "java.base"));
  }

  // Simply use the JAVA_HOME of the currently running JDK; test setup below should therefore only
  // reference resources which are present in all JDK versions used to run this test
  private static final Path JAVA_HOME = Path.of(System.getProperty("java.home"));

  private static class ExpectedDataEntry {
    private final String path;
    private final String name;

    public ExpectedDataEntry(String path, String name) {
      this.path = path;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      ExpectedDataEntry that = (ExpectedDataEntry) o;
      return path.equals(that.path) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, name);
    }

    @Override
    public String toString() {
      return "ExpectedDataEntry{path='" + path + "', name='" + name + "'}";
    }
  }

  private static class ExpectingDataEntryReader implements DataEntryReader {
    private final Set<ExpectedDataEntry> expectedEntries;
    private final Set<ExpectedDataEntry> unexpectedEntries;

    public ExpectingDataEntryReader(
        Set<ExpectedDataEntry> expectedEntries, Set<ExpectedDataEntry> unexpectedEntries) {
      this.expectedEntries = new LinkedHashSet<>(expectedEntries);
      this.unexpectedEntries = unexpectedEntries;
    }

    private long countBytes(InputStream inputStream) throws IOException {
      long skippedTotal = 0;

      while (true) {
        // Don't try `skip(Long.MAX_VALUE)`, depending on how simple the implementation of `skip`
        // is, that might trigger an OutOfMemoryError or cause numeric overflow
        long skipped = inputStream.skip(10_000);
        // Suppress IntelliJ warning for `skipped < 0`; bogus `skip` implementation could return
        // negative
        //noinspection ConstantValue
        if (skipped < 0) {
          throw new AssertionError("skip returned negative value");
        } else if (skipped == 0) {
          // Check if end of stream was reached
          if (inputStream.read() == -1) {
            return skippedTotal;
          } else {
            skippedTotal++;
          }
        } else {
          skippedTotal += skipped;
        }
      }
    }

    @Override
    public void read(DataEntry dataEntry) throws IOException {
      // Assertions which apply for all entries
      assertEquals(dataEntry.getName(), dataEntry.getOriginalName());
      assertFalse(dataEntry.isDirectory(), "unexpected directory: " + dataEntry);
      assertNull(dataEntry.getParent(), "unexpected parent for: " + dataEntry);

      ExpectedDataEntry entry = new ExpectedDataEntry(dataEntry.toString(), dataEntry.getName());
      if (expectedEntries.remove(entry)) {
        // Perform additional more expensive checks only for the specified expected entries
        InputStream inputStream = dataEntry.getInputStream();
        long bytes = countBytes(inputStream);
        dataEntry.closeInputStream();
        assertEquals(bytes, dataEntry.getSize(), "unexpected size for: " + dataEntry);
      }

      assertFalse(unexpectedEntries.contains(entry), "found unexpected entry: " + entry);
    }

    public void assertFoundAll() {
      assertTrue(expectedEntries.isEmpty(), "missing entries: " + expectedEntries);
    }
  }

  /** Entry in the {@code java.base} module */
  private static final ExpectedDataEntry JAVA_BASE_ENTRY =
      new ExpectedDataEntry(
          "jrt:/modules/java.base/java/lang/String.class", "java/lang/String.class");
  /** Entry in the {@code java.compiler} module */
  private static final ExpectedDataEntry JAVA_COMPILER_ENTRY =
      new ExpectedDataEntry(
          "jrt:/modules/java.compiler/javax/lang/model/SourceVersion.class",
          "javax/lang/model/SourceVersion.class");

  @Test
  void testCreateForAllModules() throws IOException {
    JrtDataEntrySource source = JrtDataEntrySource.tryCreate(JAVA_HOME, null);
    assertNotNull(source);

    // Should find classes from all modules
    ExpectingDataEntryReader entryReader =
        new ExpectingDataEntryReader(Set.of(JAVA_BASE_ENTRY, JAVA_COMPILER_ENTRY), Set.of());
    source.pumpDataEntries(entryReader);
    entryReader.assertFoundAll();
  }

  @Test
  void testCreateForJavaBaseModule() throws IOException {
    JrtDataEntrySource source = JrtDataEntrySource.tryCreate(JAVA_HOME, "java.base");
    assertNotNull(source);

    // Should only find classes from `java.base`
    ExpectingDataEntryReader entryReader =
        new ExpectingDataEntryReader(Set.of(JAVA_BASE_ENTRY), Set.of(JAVA_COMPILER_ENTRY));
    source.pumpDataEntries(entryReader);
    entryReader.assertFoundAll();
  }

  @Test
  void testCreateForJavaCompilerModule() throws IOException {
    JrtDataEntrySource source = JrtDataEntrySource.tryCreate(JAVA_HOME, "java.compiler");
    assertNotNull(source);

    // Should only find classes from `java.compiler`
    ExpectingDataEntryReader entryReader =
        new ExpectingDataEntryReader(Set.of(JAVA_COMPILER_ENTRY), Set.of(JAVA_BASE_ENTRY));
    source.pumpDataEntries(entryReader);
    entryReader.assertFoundAll();
  }
}
