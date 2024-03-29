/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.classfile.visitor;

import proguard.classfile.*;

/**
 * This {@link MemberVisitor} delegates all visits to a given {@link ClassVisitor}. The latter
 * visits the class of each visited class member.
 *
 * @author Eric Lafortune
 */
public class MemberToClassVisitor implements MemberVisitor {
  private final ClassVisitor classVisitor;

  public MemberToClassVisitor(ClassVisitor classVisitor) {
    this.classVisitor = classVisitor;
  }

  // Implementations for MemberVisitor.

  public void visitProgramField(ProgramClass programClass, ProgramField programField) {
    classVisitor.visitProgramClass(programClass);
  }

  public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
    classVisitor.visitProgramClass(programClass);
  }

  public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField) {
    classVisitor.visitLibraryClass(libraryClass);
  }

  public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod) {
    classVisitor.visitLibraryClass(libraryClass);
  }
}
