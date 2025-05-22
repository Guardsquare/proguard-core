package proguard.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class MapUtil {
  private MapUtil() {}

  /**
   * Constructs an immutable Map from the given key and value objects. Equivalent to the Map.of()
   * convenience method available in modern Java.
   */
  public static <K, V> Map<K, V> of(Object... keysValues) {
    if (keysValues.length % 2 != 0) {
      throw new IllegalArgumentException("You need to supply an even number of input arguments.");
    }

    Map<K, V> intermediate = new HashMap<>();
    for (int i = 0; i < keysValues.length; i += 2) {
      Object key = keysValues[i];
      Object value = keysValues[i + 1];
      intermediate.put((K) key, (V) value);
    }

    return Collections.unmodifiableMap(intermediate);
  }

  /** Visits every entry in a TreeMap where the key matches the given filter. */
  public static <T> void filterTreeMap(
      TreeMap<String, T> map, StringMatcher keyFilter, Consumer<T> consumer) {
    String prefix = keyFilter.prefix();
    if ("".equals(prefix)) {
      // It is more efficient to avoid using higherEntry when we're traversing over the whole map.
      for (Map.Entry<String, T> entry : map.entrySet()) {
        if (keyFilter.matches(entry.getKey())) {
          consumer.accept(entry.getValue());
        }
      }
    } else {
      // If we can skip towards a specific entry using the prefix and handle a smaller part of the
      // map, then it becomes worthwhile to traverse using higherEntry.
      Map.Entry<String, T> entry = map.ceilingEntry(prefix);
      while (entry != null && entry.getKey().startsWith(prefix)) {
        if (keyFilter.matches(entry.getKey())) {
          consumer.accept(entry.getValue());
        }
        entry = map.higherEntry(entry.getKey());
      }
    }
  }
}
