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
package proguard.classfile.attribute;

import proguard.classfile.Clazz;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This {@link Attribute} represents a source file attribute.
 *
 * @author Eric Lafortune
 */
public class SourceFileAttribute extends Attribute {
  public int u2sourceFileIndex;

  /** Creates an uninitialized SourceFileAttribute. */
  public SourceFileAttribute() {}

  /** Creates an initialized SourceFileAttribute. */
  public SourceFileAttribute(int u2attributeNameIndex, int u2sourceFileIndex) {
    super(u2attributeNameIndex);

    this.u2sourceFileIndex = u2sourceFileIndex;
  }

  // Implementations for Attribute.

  public void accept(Clazz clazz, AttributeVisitor attributeVisitor) {
    attributeVisitor.visitSourceFileAttribute(clazz, this);
  }
}
