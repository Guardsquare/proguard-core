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
package proguard.classfile.editor;

import java.util.HashSet;
import java.util.Set;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.ProgramMethod;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

/**
 * This visitor removes all members it visits in a {@link ProgramClass}.
 *
 * <p>It should be used in two steps:
 *
 * <ul>
 *   <li>In the first step, the collection step, all program members to be removed should be
 *       visited.
 *   <li>In the second step, the removal step, the program class containing the program members
 *       should be visited. This will actually delete all collected members.
 * </ul>
 *
 * <p>For example, to remove all fields in a program class:
 *
 * <pre>
 *     MemberRemover remover = new MemberRemover();
 *     programClass.fieldsAccept(remover);
 *     programClass.accept(remover);
 * </pre>
 *
 * @author Johan Leys
 */
public class MemberRemover implements ClassVisitor, MemberVisitor {
  private final Set<Method> methodsToRemove = new HashSet<>();
  private final Set<Field> fieldsToRemove = new HashSet<>();

  // Keep track of the class for which we're in the process of performing the first step (i.e.
  // collect all members we intend to remove).
  private Clazz currentClass;

  /**
   * Forget about all members that have been collected so far. After this method, this {@link
   * MemberRemover} will become a no-op until the next time a member is collected.
   */
  public void reset() {
    methodsToRemove.clear();
    fieldsToRemove.clear();
    currentClass = null;
  }

  // Implementations for ClassVisitor.

  @Override
  public void visitAnyClass(Clazz clazz) {}

  @Override
  public void visitProgramClass(ProgramClass programClass) {
    verifyAgainstCurrentClass(programClass);

    ClassEditor classEditor = new ClassEditor(programClass);

    // Remove all collected methods.
    for (Method method : methodsToRemove) {
      classEditor.removeMethod(method);
    }

    // Remove all collected fields.
    for (Field field : fieldsToRemove) {
      classEditor.removeField(field);
    }

    reset();
  }

  // Implementations for MemberVisitor.

  @Override
  public void visitAnyMember(Clazz clazz, Member member) {}

  @Override
  public void visitProgramField(ProgramClass programClass, ProgramField programField) {
    verifyAgainstCurrentClass(programClass);
    currentClass = programClass;
    fieldsToRemove.add(programField);
  }

  @Override
  public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
    verifyAgainstCurrentClass(programClass);
    currentClass = programClass;
    methodsToRemove.add(programMethod);
  }

  // Helper methods.

  /**
   * Throws a {@link ProguardCoreException} if <code>clazz</code> is different from the class for
   * which we're still in the process of collecting members that should be removed.
   *
   * @param clazz The {@link Clazz} to verify.
   * @throws ProguardCoreException <code>clazz</code> is different from the class for which we're
   *     still in the process of collecting members that should be removed.
   */
  private void verifyAgainstCurrentClass(Clazz clazz) throws ProguardCoreException {
    if (currentClass != null && clazz != currentClass) {
      throw new ProguardCoreException.Builder(
              "Cannot register members to remove for multiple classes at once. Commit the removal of members of the current class first before moving on to the next class.",
              ErrorId.MEMBER_REMOVER_NOT_FINISHED_WITH_CURRENT_CLASS)
          .build();
    }
  }
}
