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

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data entry source which reads JDK classes using the {@code jrt:/} file system.
 *
 * @see <a href="https://openjdk.org/jeps/220">JEP 220</a>
 */
public class JrtDataEntrySource implements DataEntrySource {
  private final Path javaHome;
  private final Path jrtFsJarPath;
  @Nullable private final String moduleName;

  public JrtDataEntrySource(Path javaHome, Path jrtFsJarPath, @Nullable String moduleName) {
    this.javaHome = javaHome;
    this.jrtFsJarPath = jrtFsJarPath;
    this.moduleName = moduleName;
  }

  /**
   * Tries to create an instance of {@code JrtDataEntrySource}. Returns {@code null} if {@code
   * javaHome} does not point to a valid JAVA_HOME which contains {@code lib/jrt-fs.jar}.
   *
   * @param javaHome path to JAVA_HOME whose classes should be read
   * @param moduleName name of the module whose classes should be read; {@code null} to read the
   *     classes of all modules
   */
  public static @Nullable JrtDataEntrySource tryCreate(Path javaHome, @Nullable String moduleName) {
    Path jrtFsJarPath = javaHome.resolve("lib").resolve("jrt-fs.jar");
    if (!Files.isRegularFile(jrtFsJarPath)) {
      return null;
    }

    return new JrtDataEntrySource(javaHome, jrtFsJarPath, moduleName);
  }

  @Override
  public void pumpDataEntries(DataEntryReader dataEntryReader) throws IOException {
    URLClassLoader fallbackClassLoader;
    try {
      // This class loader is used as fallback when the current JDK is JDK 8 and does not itself
      // have the provider for the `jrt:/` file system, see JEP 220
      fallbackClassLoader = new URLClassLoader(new URL[] {jrtFsJarPath.toUri().toURL()});
    } catch (MalformedURLException e) {
      throw new RuntimeException("Failed converting file path to URL", e);
    }

    FileSystem jrtFileSystem;
    try {
      jrtFileSystem =
          FileSystems.newFileSystem(
              URI.create("jrt:/"),
              Collections.singletonMap("java.home", javaHome.toString()),
              fallbackClassLoader);
    } catch (Exception e) {
      // Should not happen normally
      throw new IOException("Failed creating jrt:/ file system for JAVA_HOME " + javaHome, e);
    }

    // Note: Closing the class loader might not be thread-safe because by default the JDK caches the
    // underlying JarFile, so closing it implicitly here might affect other users of that jar
    // See https://bugs.openjdk.org/browse/JDK-8246714 and potentially also
    // https://bugs.openjdk.org/browse/JDK-8205976
    try (Closeable ignored = fallbackClassLoader;
        Closeable ignored2 = jrtFileSystem) {
      Path modulesPath = jrtFileSystem.getPath("modules");
      if (moduleName != null) {
        Path moduleDir = modulesPath.resolve(moduleName);
        if (!Files.isDirectory(moduleDir)) {
          throw new IOException(
              "Module '" + moduleName + "' does not exist for JDK at path " + javaHome);
        }
        processModuleFiles(moduleDir, dataEntryReader);
      } else {
        try (Stream<Path> moduleDirs = Files.list(modulesPath)) {
          for (Path moduleDir : (Iterable<? extends Path>) moduleDirs::iterator) {
            processModuleFiles(moduleDir, dataEntryReader);
          }
        }
      }
    }
  }

  private void processModuleFiles(Path moduleDir, DataEntryReader dataEntryReader)
      throws IOException {
    Files.walkFileTree(
        moduleDir,
        new SimpleFileVisitor<Path>() {
          @Override
          public @NotNull FileVisitResult visitFile(
              @NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
            String name = moduleDir.relativize(file).toString();
            dataEntryReader.read(new JrtDataEntry(file, name, attrs.size()));
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
