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
package proguard.classfile.attribute.preverification;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.preverification.visitor.StackMapFrameVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * This {@link Attribute} represents a stack map table attribute.
 *
 * @author Eric Lafortune
 */
public class StackMapTableAttribute extends Attribute {
  public int u2stackMapFramesCount;
  public StackMapFrame[] stackMapFrames;

  /** Creates an uninitialized StackMapTableAttribute. */
  public StackMapTableAttribute() {}

  /** Creates a StackMapTableAttribute with the given stack map frames. */
  public StackMapTableAttribute(StackMapFrame[] stackMapFrames) {
    this(stackMapFrames.length, stackMapFrames);
  }

  /** Creates a StackMapTableAttribute with the given stack map frames. */
  public StackMapTableAttribute(int stackMapFramesCount, StackMapFrame[] stackMapFrames) {
    this.u2stackMapFramesCount = stackMapFramesCount;
    this.stackMapFrames = stackMapFrames;
  }

  // Implementations for Attribute.

  public void accept(
      Clazz clazz, Method method, CodeAttribute codeAttribute, AttributeVisitor attributeVisitor) {
    attributeVisitor.visitStackMapTableAttribute(clazz, method, codeAttribute, this);
  }

  /** Applies the given stack map frame visitor to all stack map frames. */
  public void stackMapFramesAccept(
      Clazz clazz,
      Method method,
      CodeAttribute codeAttribute,
      StackMapFrameVisitor stackMapFrameVisitor) {
    int offset = 0;

    for (int index = 0; index < u2stackMapFramesCount; index++) {
      StackMapFrame stackMapFrame = stackMapFrames[index];

      // Note that the byte code offset is computed differently for the
      // first stack map frame.
      offset += stackMapFrame.getOffsetDelta() + (index == 0 ? 0 : 1);

      stackMapFrame.accept(clazz, method, codeAttribute, offset, stackMapFrameVisitor);
    }
  }
}
