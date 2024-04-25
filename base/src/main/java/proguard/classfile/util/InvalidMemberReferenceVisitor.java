package proguard.classfile.util;

import proguard.classfile.Clazz;
import proguard.classfile.LibraryClass;
import proguard.classfile.ProgramClass;

/** A visitor for invalid member references. */
public interface InvalidMemberReferenceVisitor {
  /**
   * Visit a missing field or method reference.
   *
   * @param referencingClazz the class from which the field or method is referenced.
   * @param reference the referenced class.
   * @param name the name of the missing referenced field or method.
   * @param type the type of the missing field or method.
   */
  void visitAnyMissingMember(Clazz referencingClazz, Clazz reference, String name, String type);

  /**
   * Visit a missing field reference.
   *
   * @param referencingClazz the class from which the field is referenced.
   * @param reference the referenced class.
   * @param name the name of the missing referenced field.
   * @param type the type of the missing referenced field.
   */
  default void visitAnyMissingField(
      Clazz referencingClazz, Clazz reference, String name, String type) {
    visitAnyMissingMember(referencingClazz, reference, name, type);
  }

  default void visitMissingProgramField(
      Clazz referencingClazz, ProgramClass reference, String name, String type) {
    visitAnyMissingField(referencingClazz, reference, name, type);
  }

  default void visitMissingLibraryField(
      Clazz referencingClazz, LibraryClass reference, String name, String type) {
    visitAnyMissingField(referencingClazz, reference, name, type);
  }

  /**
   * Visit a missing method.
   *
   * @param referencingClazz the class from which the method is referenced.
   * @param reference the referenced class.
   * @param name the name of the missing referenced method.
   * @param type the type of the missing referenced method.
   */
  default void visitAnyMissingMethod(
      Clazz referencingClazz, Clazz reference, String name, String type) {
    visitAnyMissingMember(referencingClazz, reference, name, type);
  }

  default void visitMissingProgramMethod(
      Clazz referencingClazz, ProgramClass reference, String name, String type) {
    visitAnyMissingMethod(referencingClazz, reference, name, type);
  }

  default void visitMissingLibraryMethod(
      Clazz referencingClazz, LibraryClass reference, String name, String type) {
    visitAnyMissingMethod(referencingClazz, reference, name, type);
  }

  /**
   * Visit a missing enclosing method.
   *
   * @param enclosingClazz the class containing the enclosing method.
   * @param reference the referenced class that encloses the method.
   * @param name the name of the enclosing method.
   * @param type the type of the enclosing method.
   */
  default void visitMissingEnclosingMethod(
      Clazz enclosingClazz, Clazz reference, String name, String type) {
    visitAnyMissingMethod(enclosingClazz, reference, name, type);
  }
}
