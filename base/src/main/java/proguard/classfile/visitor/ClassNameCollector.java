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

import java.util.Collection;
import proguard.classfile.Clazz;

/**
 * This {@link ClassVisitor} collects the names of the classes that it visits in the given
 * collection.
 *
 * @author Eric Lafortune
 */
public class ClassNameCollector implements ClassVisitor {
  private final Collection<String> collection;

  /**
   * Creates a new ClassNameCollector.
   *
   * @param collection the <code>Collection</code> in which all class names will be collected.
   */
  public ClassNameCollector(Collection<String> collection) {
    this.collection = collection;
  }

  // Implementations for ClassVisitor.

  public void visitAnyClass(Clazz clazz) {
    collection.add(clazz.getName());
  }
}
