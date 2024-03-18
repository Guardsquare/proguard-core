package proguard.classfile.util;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.Clazz;
import proguard.classfile.visitor.ClassCounter;
import proguard.classfile.visitor.ClassNameFilter;

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

  private InitializedClassUtil() {}

  /**
   * Returns true if the given type is instance of the given clazz (i.e. whether the class is the
   * same type or an extension of the given type).
   *
   * <p>If the type is a reference array checks the inheritance of the referenced type (e.g. true
   * for "[Ljava/lang/String;" and Clazz{java/lang/Object}, false for "[Ljava/lang/String;" and
   * Clazz{java/lan/Boolean}).
   *
   * <p>Requires that at least {@link ClassReferenceInitializer} and {@link
   * ClassSubHierarchyInitializer} have been run on the {@link proguard.classfile.ClassPool}s.
   */
  public static boolean isInstanceOf(String type, @NotNull Clazz clazz) {
    Objects.requireNonNull(clazz);
    ClassCounter counter = new ClassCounter();

    clazz.hierarchyAccept(
        true,
        false,
        false,
        true,
        new ClassNameFilter(
            ClassUtil.internalClassName(ClassUtil.externalBaseType(ClassUtil.externalType(type))),
            counter));
    return counter.getCount() > 0;
  }
}
