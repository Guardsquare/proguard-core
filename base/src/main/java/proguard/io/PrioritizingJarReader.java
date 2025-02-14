package proguard.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import proguard.util.FileNameParser;
import proguard.util.ListParser;
import proguard.util.StringMatcher;

/**
 * This {@link JarReader} lets a given {@link DataEntryReader} read all data entries of the read
 * archive data entries in an order that respects a set of given priorities.
 */
public class PrioritizingJarReader extends JarReader {
  private final DataEntryReader dataEntryReader;
  private final Map<StringMatcher, Integer> matcherToPriorityMap = new HashMap<>();

  /**
   * Creates a new {@link PrioritizingJarReader} that delegates reads to the given {@link
   * DataEntryReader} in the order decided by the given priority map.
   *
   * @param priorityMap a map linking filters to priority values. Lower priority values mean higher
   *     reading priority. The priority of entries that don't match any filters defaults to 0.
   * @param dataEntryReader the reader that can process the jar entries.
   */
  public PrioritizingJarReader(Map<String, Integer> priorityMap, DataEntryReader dataEntryReader) {
    this(priorityMap, false, dataEntryReader);
  }

  /**
   * Creates a new {@link PrioritizingJarReader} that delegates reads to the given {@link
   * DataEntryReader} in the order decided by the given priority map. This reader can optionally
   * read jmod files.
   *
   * @param priorityMap a map linking filters to priority values. Lower priority values mean higher
   *     reading priority. The priority of entries that don't match any filters defaults to 0.
   * @param jmod specifies whether the input jar is actually a jmod file.
   * @param dataEntryReader the reader that can process the jar entries.
   */
  public PrioritizingJarReader(
      Map<String, Integer> priorityMap, boolean jmod, DataEntryReader dataEntryReader) {
    super(jmod, dataEntryReader);
    this.dataEntryReader = dataEntryReader;
    priorityMap.forEach(
        (filter, priority) ->
            this.matcherToPriorityMap.put(
                new ListParser(new FileNameParser()).parse(filter), priority));
  }

  // Implementation for DataEntryReader.

  @Override
  public void read(DataEntry dataEntry) throws IOException {
    // Can we parse the jar entries more robustly from a file?
    if (dataEntry instanceof FileDataEntry) {
      // Read the data entry using its file.
      FileDataEntry fileDataEntry = (FileDataEntry) dataEntry;

      ZipFile zipFile = new ZipFile(fileDataEntry.getFile(), StandardCharsets.UTF_8);

      try {
        Enumeration entries = zipFile.entries();
        List<ComparableZipEntry> orderedZipEntryList = new ArrayList<>();

        // Get all entries from the input jar.
        while (entries.hasMoreElements()) {
          ZipEntry zipEntry = (ZipEntry) entries.nextElement();
          orderedZipEntryList.add(new ComparableZipEntry(zipEntry, getPriority(zipEntry)));
        }

        // Reorder entries based on their priorities. This is a stable sort, meaning that elements
        // with the same priority are not reordered.
        Collections.sort(orderedZipEntryList);

        for (ComparableZipEntry entry : orderedZipEntryList) {
          // Delegate the actual reading to the data entry reader.
          dataEntryReader.read(new ZipFileDataEntry(dataEntry, entry.getZipEntry(), zipFile));
        }
      } finally {
        zipFile.close();
      }
    } else {
      // We only support prioritized reading of entries from a data entry representing a file.
      super.read(dataEntry);
    }
  }

  /**
   * Calculates and returns the priority of a given zipEntry.
   *
   * <ul>
   *   <li>If the entry matches at least one of the {@link StringMatcher}s in the priority map, the
   *       lowest priority of all matching filters is returned.
   *   <li>If the entry matches none of the {@link StringMatcher}s, the default priority (0) is
   *       returned.
   * </ul>
   *
   * @param zipEntry the entry to get the priority of.
   * @return the priority of the given zipEntry.
   */
  private int getPriority(ZipEntry zipEntry) {
    int priority = Integer.MAX_VALUE;
    boolean matched = false;
    for (Map.Entry<StringMatcher, Integer> entry : matcherToPriorityMap.entrySet()) {
      if (entry.getKey().matches(zipEntry.getName()) && entry.getValue() < priority) {
        matched = true;
        priority = entry.getValue();
      }
    }
    return matched ? priority : 0;
  }

  // Utility class to allow ordering zip entries based on their priority. Lower priority values
  // indicate a higher reading priority.
  private static class ComparableZipEntry
      implements Comparable<PrioritizingJarReader.ComparableZipEntry> {

    private final ZipEntry zipEntry;
    private final int priority;

    public ComparableZipEntry(ZipEntry zipEntry, int priority) {
      this.zipEntry = zipEntry;
      this.priority = priority;
    }

    public ZipEntry getZipEntry() {
      return zipEntry;
    }

    public int compareTo(@NotNull PrioritizingJarReader.ComparableZipEntry other) {
      return Integer.compare(this.priority, other.priority);
    }
  }
}
