package proguard.util;

import java.util.Set;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.Clazz;

/**
 * And interface to be implemented by classes that are able to provide information on the hierarchy
 * of classes.
 *
 * <p>For example implementations could simply use a {@link
 * proguard.classfile.visitor.ClassHierarchyTraveler} or provide faster access by pre-computing the
 * class hierarchy.
 */
public interface HierarchyProvider {
  /**
   * @param className name of the {@link Clazz}.
   * @return The {@link Clazz} object.
   */
  @Nullable
  Clazz getClazz(String className);

  /**
   * @param className name of the parent {@link Clazz}.
   * @return A set of names of the subclasses, not including the parent {@link Clazz}'s.
   */
  Set<String> getSubClasses(String className);
}
