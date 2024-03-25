/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

import org.jetbrains.annotations.Nullable;
import proguard.classfile.Clazz;
import proguard.classfile.LibraryClass;
import proguard.classfile.LibraryMethod;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.MethodDescriptor;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.classfile.util.ClassUtil;

/**
 * Divides the referenced classes in a program/library method in the classes referenced in return
 * and parameters.
 */
public class ReferencedClassesExtractor implements MemberVisitor {

  private Clazz returnClass = null;
  private Clazz[] parameterClasses = null;

  @Override
  public void visitAnyMember(Clazz clazz, Member member) {
    // only interested in program and library methods
  }

  @Override
  public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {

    extractReferencedClasses(programClass, programMethod, programMethod.referencedClasses);
  }

  @Override
  public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod) {
    extractReferencedClasses(libraryClass, libraryMethod, libraryMethod.referencedClasses);
  }

  private void extractReferencedClasses(Clazz clazz, Method method, Clazz[] referencedClasses) {
    MethodDescriptor descriptor = new MethodDescriptor(method.getDescriptor(clazz));

    parameterClasses = new Clazz[descriptor.getArgumentTypes().size()];

    if (referencedClasses == null || referencedClasses.length == 0) {
      return;
    }

    if (!isPrimitiveOrPrimitiveArrayType(descriptor.getReturnType())) {
      this.returnClass = referencedClasses[referencedClasses.length - 1];
    }

    int referencedClassesIndex = 0;

    for (int i = 0; i < descriptor.getArgumentTypes().size(); i++) {
      String argumentType = descriptor.getArgumentTypes().get(i);
      if (!isPrimitiveOrPrimitiveArrayType(argumentType)) {
        getParameterClasses()[i] = referencedClasses[referencedClassesIndex++];
      }
    }
  }

  private boolean isPrimitiveOrPrimitiveArrayType(String type) {
    return !ClassUtil.isInternalClassType(type);
  }

  /**
   * Returns the referenced return {@link Clazz} of the target method.
   *
   * @return The instance referenced class. Null if the return type is a primitive or array of
   *     primitives. Can be null if the class pools have not been initialized; even if they have,
   *     the clazz not being null is not a guarantee.
   */
  public @Nullable Clazz getReturnClass() {
    return returnClass;
  }

  /**
   * Returns the referenced {@link Clazz} for each parameter.
   *
   * @return The referenced class for each parameter (instance excluded), where each parameter can
   *     be accessed given its position (starting from 0, category 2 values take only one slot). An
   *     element is null if the corresponding parameter is of a primitive type or an array of
   *     primitives. An element can be null if the class pools have not been initialized; even if
   *     they have, elements not being null is not a guarantee
   */
  public Clazz[] getParameterClasses() {
    return parameterClasses;
  }
}
