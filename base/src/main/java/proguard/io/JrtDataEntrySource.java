package proguard.io;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

/**
 * Data entry source which reads JDK classes using the {@code jrt:/} file system.
 *
 * @see <a href="https://openjdk.org/jeps/220">JEP 220</a>
 */
public class JrtDataEntrySource implements DataEntrySource {
  @Nullable private final String moduleName;
  @NotNull private final FileSystem jrtFileSystem;

  /**
   * @param javaHome path to JAVA_HOME whose classes should be read
   * @param moduleName name of the module whose classes should be read; {@code null} to read the
   *     classes of all modules
   */
  public JrtDataEntrySource(Path javaHome, @Nullable String moduleName) {
    this.moduleName = moduleName;

    Path jrtFsJarPath = getJrtFsJarPath(javaHome);
    if (!Files.isRegularFile(jrtFsJarPath)) {
      throw new ProguardCoreException.Builder(
              "%s does not look like a JAVA HOME: missing file %s", ErrorId.JRT_INVALID_JAVA_HOME)
          .errorParameters(javaHome, jrtFsJarPath)
          .build();
    }
    try {
      // This class loader is used as fallback when the current JDK is JDK 8 and does not itself
      // have the
      // provider for the `jrt:/` file system
      URLClassLoader fallbackClassLoader =
          new URLClassLoader(new URL[] {jrtFsJarPath.toUri().toURL()});
      jrtFileSystem =
          FileSystems.newFileSystem(
              URI.create("jrt:/"),
              Collections.singletonMap("java.home", javaHome.toString()),
              fallbackClassLoader);
    } catch (Exception e) {
      // Should not happen normally
      throw new ProguardCoreException.Builder(
              "Unexpected error when retrieving the JRT file system", ErrorId.JRT_FILE_SYSTEM_ERROR)
          .cause(e)
          .build();
    }
  }

  @Override
  public void pumpDataEntries(DataEntryReader dataEntryReader) throws IOException {
    Path modulesPath = jrtFileSystem.getPath("modules");
    if (moduleName != null) {
      Path moduleDir = modulesPath.resolve(moduleName);
      if (!Files.isDirectory(moduleDir)) {
        throw new ProguardCoreException.Builder(
                "JRT module '%s' does not exist", ErrorId.JRT_INVALID_MODULE)
            .errorParameters(moduleName)
            .build();
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

  /** Gets the path for the {@code jrt-fs.jar}. */
  public static Path getJrtFsJarPath(Path javaHome) {
    return javaHome.resolve("lib").resolve("jrt-fs.jar");
  }
}
