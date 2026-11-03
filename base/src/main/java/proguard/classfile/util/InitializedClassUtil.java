package proguard.classfile.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.Clazz;

/**
 * Utility methods for operation on {@link Clazz} that require initialization of the {@link
 * proguard.classfile.ClassPool}s to work (i.e., {@link ClassReferenceInitializer}, {@link
 * ClassSubHierarchyInitializer}, and {@link ClassSuperHierarchyInitializer} should have run on the
 * class pools).
 *
 * <p>The methods provide no guarantee that the initialization has been performed, the callers
 * should make sure of it to avoid unexpected behavior or crashes.
 */
public class InitializedClassUtil {

  /**
   * Memoization cache holding the {@link #isInstanceOf} results, keyed by type and clazz. Results
   * only depend on the (initialized) class pools, so they can be reused across calls and threads.
   * The clazz keys retain their {@link Clazz} objects until {@link #clearCache} is called, similar
   * to the signature caches ({@link proguard.classfile.ClassSignature}, {@link
   * proguard.classfile.MethodSignature}).
   */
  private static final Map<Pair, Boolean> IS_INSTANCE_OF_CACHE = new ConcurrentHashMap<>();

  private InitializedClassUtil() {}

  /**
   * Clears the {@link #isInstanceOf} cache ({@link #IS_INSTANCE_OF_CACHE}). Should be called if the
   * class pools are (re-)initialized after {@link #isInstanceOf} results have been cached for them.
   */
  public static void clearCache() {
    IS_INSTANCE_OF_CACHE.clear();
  }

  /**
   * Returns true if the given type is instance of the given clazz (i.e. whether the class is the
   * same type or an extension of the given type).
   *
   * <p>If the type is a reference array checks the inheritance of the referenced type (e.g. true
   * for "[Ljava/lang/String;" and Clazz{java/lang/Object}, false for "[Ljava/lang/String;" and
   * Clazz{java/lang/Boolean}).
   *
   * <p>Requires that at least {@link ClassReferenceInitializer} and {@link
   * ClassSubHierarchyInitializer} have been run on the {@link proguard.classfile.ClassPool}s.
   */
  public static boolean isInstanceOf(String type, @NotNull Clazz clazz) {
    Pair key = new Pair(type, clazz);
    Boolean cached = IS_INSTANCE_OF_CACHE.get(key);
    if (cached != null) {
      return cached;
    }
    boolean result = isInstanceOfWithoutCache(type, clazz);
    IS_INSTANCE_OF_CACHE.put(key, result);
    return result;
  }

  /**
   * Walks the subclass tree of clazz with a visited set and early exit to determine whether any
   * subclass matches the given type.
   */
  private static boolean isInstanceOfWithoutCache(String type, @NotNull Clazz clazz) {
    String target =
        ClassUtil.internalClassName(ClassUtil.externalBaseType(ClassUtil.externalType(type)));
    Set<Clazz> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<Clazz> worklist = new ArrayDeque<>();
    worklist.add(clazz);
    while (!worklist.isEmpty()) {
      Clazz current = worklist.removeFirst();
      if (!visited.add(current)) {
        continue;
      }
      if (target.equals(current.getName())) {
        return true;
      }
      current.subclassesAccept(worklist::add);
    }
    return false;
  }

  /**
   * Cache key for {@link #IS_INSTANCE_OF_CACHE}, holding a type and a clazz. The clazz is compared
   * by identity, as {@link Clazz} implementations use default {@link Object} equality.
   */
  private static final class Pair {

    private final String type;
    private final Clazz clazz;

    Pair(String type, Clazz clazz) {
      this.type = type;
      this.clazz = clazz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Pair)) {
        return false;
      }
      Pair pair = (Pair) o;
      return type.equals(pair.type) && clazz == pair.clazz;
    }

    @Override
    public int hashCode() {
      return 31 * type.hashCode() + System.identityHashCode(clazz);
    }
  }
}
