/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

import java.util.Collection;
import proguard.classfile.Clazz;

/**
 * This <code>ClassVisitor</code> collects the feature names of the classes that it visits in the
 * given collection.
 *
 * @author Eric Lafortune
 */
public class ClassFeatureNameCollector implements ClassVisitor {
  private final Collection<String> collection;

  /**
   * Creates a new ClassNameCollector.
   *
   * @param collection the Collection in which all feature names will be collected.
   */
  public ClassFeatureNameCollector(Collection<String> collection) {
    this.collection = collection;
  }

  // Implementations for ClassVisitor.

  public void visitAnyClass(Clazz clazz) {
    collection.addAll(clazz.getExtraFeatureNames());
  }
}
