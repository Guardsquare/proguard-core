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
import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.MemberVisitor;

/**
 * This {@link MemberVisitor} fixes all inappropriate bridge access flags of the program methods
 * that it visits, checking whether the methods to which they bridge have the same name. Some
 * compilers, like in Eclipse and in later versions of JDK 1.6, complain if they can't find the
 * method with the same name.
 *
 * @author Eric Lafortune
 */
public class BridgeMethodFixer
    implements MemberVisitor, AttributeVisitor, InstructionVisitor, ConstantVisitor {
  private static final boolean DEBUG = false;

  // Used to store the methods invoked by the bridge method.
  // If the bridged method is not in here, then the bridge flag will be cleared.
  private final Set<String> bridgeInvokedMethodNames = new HashSet<>();

  // Implementations for MemberVisitor.

  public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
    if ((programMethod.getAccessFlags() & AccessConstants.BRIDGE) != 0) {
      bridgeInvokedMethodNames.clear();
      programMethod.attributesAccept(programClass, this);

      // The bridge method must contain a call to the bridged's method.
      // Otherwise, remove the bridge flag.
      if (!bridgeInvokedMethodNames.contains(programMethod.getName(programClass))) {
        if (DEBUG) {
          System.out.println(
              "BridgeMethodFixer: ["
                  + programClass.getName()
                  + "."
                  + programMethod.getName(programClass)
                  + programMethod.getDescriptor(programClass)
                  + "] does not bridge to its implementation");
        }

        // Clear the bridge flag.
        programMethod.u2accessFlags &= ~AccessConstants.BRIDGE;
      }
    }
  }

  // Implementations for AttributeVisitor.

  public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

  public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
    // Go over the instructions of the bridge method.
    codeAttribute.instructionsAccept(clazz, method, this);
  }

  // Implementations for InstructionVisitor.

  public void visitAnyInstruction(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      Instruction instruction) {}

  public void visitConstantInstruction(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      int offset,
      ConstantInstruction constantInstruction) {
    switch (constantInstruction.opcode) {
      case Instruction.OP_INVOKEVIRTUAL:
      case Instruction.OP_INVOKESPECIAL:
      case Instruction.OP_INVOKESTATIC:
      case Instruction.OP_INVOKEINTERFACE:
        // Get the names of the invoked methods.
        clazz.constantPoolEntryAccept(constantInstruction.constantIndex, this);
        break;
    }
  }

  // Implementations for ConstantVisitor.

  public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant) {
    bridgeInvokedMethodNames.add(anyMethodrefConstant.getName(clazz));
  }
}
