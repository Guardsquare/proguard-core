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
package proguard.classfile.instruction.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This {@link AttributeVisitor} lets a given {@link InstructionVisitor} visit all Instruction
 * instances of the {@link CodeAttribute} instances it visits.
 *
 * @author Eric Lafortune
 */
public class AllInstructionVisitor implements AttributeVisitor {
  private final InstructionVisitor instructionVisitor;

  public AllInstructionVisitor(InstructionVisitor instructionVisitor) {
    this.instructionVisitor = instructionVisitor;
  }

  // Implementations for AttributeVisitor.

  public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

  public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
    codeAttribute.instructionsAccept(clazz, method, instructionVisitor);
  }
}
