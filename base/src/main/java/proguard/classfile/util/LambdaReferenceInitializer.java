/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2025 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.classfile.util;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.ProgramClass;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.InvokeDynamicConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.visitor.ClassVisitor;

/**
 * This {@link ClassVisitor} initializes the references of {@link InvokeDynamicConstant}s
 * that represent lambda expressions. More specifically, it links the constants to the
 * actual functional interface methods they target in the program class pool or in the
 * library class pool.
 *
 * <p>The class hierarchy must be initialized before using this visitor.
 *
 * @author ShortyDev
 */
public class LambdaReferenceInitializer
        implements ClassVisitor,
        ConstantVisitor {
  private final ClassPool programClassPool;
  private final ClassPool libraryClassPool;

  public LambdaReferenceInitializer(ClassPool programClassPool, ClassPool libraryClassPool) {
    this.programClassPool = programClassPool;
    this.libraryClassPool = libraryClassPool;
  }

  // Implementations for ClassVisitor.
  @Override
  public void visitAnyClass(Clazz clazz) {
  }

  @Override
  public void visitProgramClass(ProgramClass programClass) {
    programClass.constantPoolEntriesAccept(this);
  }

  // Implementations for ConstantVisitor.
  @Override
  public void visitAnyConstant(Clazz clazz, Constant constant) {
  }

  @Override
  public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant) {
    // Get the descriptor to find the return type.
    String descriptor = invokeDynamicConstant.getType(clazz);
    String returnType = ClassUtil.internalMethodReturnType(descriptor);

    // Check if the return type is a class type.
    if (ClassUtil.isInternalClassType(returnType)) {
      String interfaceClassName = ClassUtil.internalClassNameFromClassType(returnType);

      // Find the interface class.
      Clazz interfaceClass = programClassPool.getClass(interfaceClassName);

      if (interfaceClass == null) {
        interfaceClass = libraryClassPool.getClass(interfaceClassName);
      }

      if (interfaceClass != null) {
        String methodName = invokeDynamicConstant.getName(clazz);

        // Find the method in the interface.
        Member referencedMember = interfaceClass.findMethod(methodName, null);

        if (referencedMember != null) {
          invokeDynamicConstant.referencedClass = interfaceClass;
          invokeDynamicConstant.referencedMember = referencedMember;
        }
      }
    }
  }
}