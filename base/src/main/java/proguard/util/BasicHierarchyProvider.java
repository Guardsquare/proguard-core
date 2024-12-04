package proguard.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.visitor.ClassHierarchyTraveler;

/**
 * Basic implementation of {@link HierarchyProvider}, walking the class pools every time the
 * sub-classes of a class are needed. If there is need to compute the hierarchy for a class multiple
 * times and performance is a concern, rely on an implementation precomputing or caching the class
 * hierarchy.
 */
public class BasicHierarchyProvider implements HierarchyProvider {

  private final ClassPool programClassPool;
  private final ClassPool libraryClassPool;

  public BasicHierarchyProvider(ClassPool programClassPool, ClassPool libraryClassPool) {
    this.programClassPool = programClassPool;
    this.libraryClassPool = libraryClassPool;
  }

  @Override
  public Clazz getClazz(String className) {
    Clazz clazz = programClassPool.getClass(className);
    if (clazz == null) clazz = libraryClassPool.getClass(className);
    return clazz;
  }

  @Override
  public Set<String> getSubClasses(String className) {
    HashSet<Clazz> subClasses = new HashSet<>();
    ClassHierarchyTraveler subClassSignatureCollector =
        new ClassHierarchyTraveler(false, false, false, true, subClasses::add);
    programClassPool.classAccept(className, subClassSignatureCollector);
    libraryClassPool.classAccept(className, subClassSignatureCollector);
    return subClasses.stream().map(Clazz::getName).collect(Collectors.toSet());
  }
}
