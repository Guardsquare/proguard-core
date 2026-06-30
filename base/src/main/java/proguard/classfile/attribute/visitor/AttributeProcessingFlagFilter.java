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
package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;

/**
 * This {@link AttributeVisitor} delegates its visits to another given {@link AttributeVisitor}, but
 * only when the visited attribute has the proper processing flags.
 *
 * @author Johan Leys
 */
public class AttributeProcessingFlagFilter implements AttributeVisitor {
  private final int requiredSetProcessingFlags;
  private final int requiredUnsetProcessingFlags;
  private final AttributeVisitor acceptedAttributeVisitor;
  private final AttributeVisitor rejectedAttributeVisitor;

  /**
   * Creates a new AttributeProcessingFlagFilter.
   *
   * @param requiredSetProcessingFlags the attribute processing flags that should be set.
   * @param requiredUnsetProcessingFlags the attribute processing flags that should be unset.
   * @param attributeVisitor the <code>AttributeVisitor</code> to which visits will be delegated.
   */
  public AttributeProcessingFlagFilter(
      int requiredSetProcessingFlags,
      int requiredUnsetProcessingFlags,
      AttributeVisitor attributeVisitor) {
    this(requiredSetProcessingFlags, requiredUnsetProcessingFlags, attributeVisitor, null);
  }

  /**
   * Creates a new AttributeProcessingFlagFilter.
   *
   * @param requiredSetProcessingFlags the attribute processing flags that should be set.
   * @param requiredUnsetProcessingFlags the attribute processing flags that should beunset.
   * @param acceptedAttributeVisitor the <code>AttributeVisitor</code> to which visits will be
   *     delegated.
   * @param rejectedAttributeVisitor the <code>AttributeVisitor</code> to which visits of attributes
   *     that do not have the proper flags will be delegated.
   */
  public AttributeProcessingFlagFilter(
      int requiredSetProcessingFlags,
      int requiredUnsetProcessingFlags,
      AttributeVisitor acceptedAttributeVisitor,
      AttributeVisitor rejectedAttributeVisitor) {
    this.requiredSetProcessingFlags = requiredSetProcessingFlags;
    this.requiredUnsetProcessingFlags = requiredUnsetProcessingFlags;
    this.acceptedAttributeVisitor = acceptedAttributeVisitor;
    this.rejectedAttributeVisitor = rejectedAttributeVisitor;
  }

  // Implementations for AttributeVisitor.

  @Override
  public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

  @Override
  public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) {
    AttributeVisitor delegateVisitor = getDelegateVisitor(codeAttribute.processingFlags);
    if (delegateVisitor != null) {
      codeAttribute.accept(clazz, method, delegateVisitor);
    }
  }

  // Small utility methods.

  private AttributeVisitor getDelegateVisitor(int processingFlags) {
    return accepted(processingFlags) ? acceptedAttributeVisitor : rejectedAttributeVisitor;
  }

  private boolean accepted(int processingFlags) {
    return (requiredSetProcessingFlags & ~processingFlags) == 0
        && (requiredUnsetProcessingFlags & processingFlags) == 0;
  }
}
