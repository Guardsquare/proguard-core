package proguard.util;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hierarchical lookup table for pattern matching. Keys with wildcards can be inserted and during
 * lookup, the wildcards will be honored. E.g. you can insert "A:*:*" and during lookup of "A:B:C",
 * the wildcard value will be returned (unless there exists e.g. "A:B:*").
 *
 * <h3>Complexity</h3>
 *
 * <p>We consider the keys <i>well-behaved</i>, when the wildcards are at the end and never before
 * any specific value. E.g. "A:*:*" is well-behaved while "*:*:A" is not.
 *
 * <p>For <i>well-behaved</i> keys, the accessor methods run in O(hierarchy depth). Without this
 * property, the lookups can at worst take O(number of entries).
 *
 * @param <K> Type of the used key
 * @param <V> Type of the value associated with each key
 * @param <C> Type of the constituents that the key is composed of
 */
public final class HierarchicalWildcardMap<K, V, C> {
  private static final Logger log = LogManager.getLogger(HierarchicalWildcardMap.class);

  /**
   * The underlying data structure used for lookups. It is actually a hash map, whose values are
   * either another HashMap or the end value. The first 'depth-1' layers all contain hash maps, the
   * last layer of the tree always contains the values themselves.
   */
  private final HashMap<C, Object> maps;

  private final int depth;
  private final Function<K, C[]> disassembler;
  private final C wildcard;

  /**
   * Constructor for the hierarchical wildcard map.
   *
   * @param depth Constant depth of the lookup hierarchy.
   * @param disassembler Function to split the key into its individual constituents. The result must
   *     always be a non-null array with length 'depth'.
   * @param wildcard A value for a key constituent indicating a wildcard. Null can be used as a
   *     wildcard.
   */
  public HierarchicalWildcardMap(int depth, Function<K, C[]> disassembler, C wildcard) {
    this.maps = new HashMap<>();
    this.depth = depth;
    this.disassembler = disassembler;
    this.wildcard = wildcard;
  }

  /**
   * Insert a key into the map. The key can contain wildcards.
   *
   * @param key The key to be inserted.
   * @param value The value associated with the key.
   */
  public void put(@NotNull K key, @Nullable V value) {
    C[] constituents = disassembler.apply(key);
    if (constituents.length != this.depth) {
      throw new IllegalStateException(
          "The provided disassembler function must always return the constituents array with a fixed size of "
              + this.depth);
    }

    HashMap<C, Object> currentMap = maps;
    for (int i = 0; i < this.depth - 1; i++) {
      currentMap =
          (HashMap<C, Object>)
              currentMap.computeIfAbsent(constituents[i], unused -> new HashMap<C, Object>());
    }
    currentMap.put(constituents[this.depth - 1], value);
  }

  /**
   * Perform a lookup on the given key, honour wildcards both in the lookup key and in the stored
   * data structure.
   *
   * <p>When wildcards are present in the lookup key, worst case runtime is in O(number of
   * mappings). If the keys are well-behaved (see class for definition), runs in O(depth).
   *
   * <p>When wildcards are not present in the key, worst case runtime is O(2^depth). For
   * well-behaved keys, it is O(depth)
   *
   * @param key The lookup key
   * @return A value corresponding to the key, null otherwise
   */
  public @Nullable V get(@NotNull K key) {
    C[] constituents = disassembler.apply(key);
    if (constituents.length != this.depth) {
      throw new IllegalStateException(
          "The provided disassembler function must always return the constituents array with a fixed size of "
              + this.depth);
    }

    // run the recursive query implementation
    return this.getImpl(0, constituents, this.maps);
  }

  /**
   * Recursive implementation of {@link #get(K)}. Handles backtracking of the lookup due to
   * non-well-behaved keys though the recursion.
   *
   * @param currentDepth Current level of recursion in the hierarchy. Should start with 0
   * @param keyConstituents The key that we are currently trying to match.
   * @param currentLevel The result of a previous level of lookup. Can be either a HashMap, or the
   *     value type. Should start with <code>this.maps</code>
   */
  private @Nullable V getImpl(int currentDepth, C[] keyConstituents, Object currentLevel) {
    // Check, that we are on the deepest level of the hierarchy (i.e. currentLevel would be the
    // resulting value)
    if (currentDepth == this.depth) {
      return (V) currentLevel;
    }

    // Now we know that we are not on the lowest level, so the `currentLevel` is always a map
    HashMap<C, Object> map = (HashMap<C, Object>) currentLevel;

    // The map can be null, if we asked for an invalid key one level higher. In such a case, report
    // failure by returning null. The levels above will retry with a different key or return
    // null indicating that this map does not contain the queried key
    if (map == null) {
      return null;
    }

    // decide on which recursive implementation to take in this level of the hierarchy
    // based on whether the current constituent is a wildcard or not
    if (Objects.equals(keyConstituents[currentDepth], this.wildcard)) {
      return handleWildcardQuery(currentDepth, keyConstituents, map);
    } else {
      return handleNonWildcardQuery(currentDepth, keyConstituents, map);
    }
  }

  private @Nullable V handleNonWildcardQuery(
      int currentDepth, C[] keyConstituents, HashMap<C, Object> map) {
    // The current level lookup key is not a wildcard, so we:
    //   1. Try to find an exact match
    //   2. If that did not work, we try if the table has a wildcard and proceed with that
    Object next = map.get(keyConstituents[currentDepth]);
    @Nullable V result = this.getImpl(currentDepth + 1, keyConstituents, next);
    if (result == null) {
      return this.getImpl(currentDepth + 1, keyConstituents, map.get(this.wildcard));
    } else {
      return result;
    }
  }

  private @Nullable V handleWildcardQuery(
      int currentDepth, C[] keyConstituents, HashMap<C, Object> map) {
    // The current level lookup is a wildcard lookup. We proceed by:
    //   1. Trying if there exists an explicitly stored wildcard key
    //   2. If that does not work, we try to proceed with all non-wildcard keys.
    //      We can do that, because the lookup key itself is a wildcard

    // step 1 from above
    if (map.containsKey(this.wildcard)) {
      Object next = map.get(keyConstituents[currentDepth]);
      @Nullable V result = this.getImpl(currentDepth + 1, keyConstituents, next);
      if (result != null) {
        return result;
      }
    }

    // step 2 from above
    for (Object value : map.values()) {
      @Nullable V result = this.getImpl(currentDepth + 1, keyConstituents, value);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Check, whether the map contains given lookup key while honouring the stored wildcards and the
   * wildcards in the key.
   *
   * <p>Same complexity as {@link #get(K)}
   *
   * @param key The lookup key.
   * @return True if there exists a value for the key.
   */
  public boolean containsKey(@NotNull K key) {
    return this.get(key) != null;
  }
}
