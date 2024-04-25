package proguard.classfile.util;

import proguard.classfile.Clazz;

/** A visitor that is visited for invalid class references. */
public interface InvalidClassReferenceVisitor {
  /**
   * Visit a missing class.
   *
   * @param referencingClazz the class from which the reference is made.
   * @param reference the name of the referenced class that was not found.
   */
  void visitMissingClass(Clazz referencingClazz, String reference);

  /**
   * Visit a library class depending on a program class.
   *
   * @param referencingClazz the library class.
   * @param dependency the program class that is referenced by <code>referencingClazz</code>
   */
  void visitProgramDependency(Clazz referencingClazz, Clazz dependency);
}
