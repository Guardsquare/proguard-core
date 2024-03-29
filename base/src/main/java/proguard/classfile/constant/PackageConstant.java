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
package proguard.classfile.constant;

import proguard.classfile.*;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This {@link Constant} represents a package constant in the constant pool.
 *
 * @author Joachim Vandersmissen
 */
public class PackageConstant extends Constant {
  public int u2nameIndex;

  /** Creates an uninitialized PackageConstant. */
  public PackageConstant() {}

  /**
   * Creates a new PackageConstant with the given name index.
   *
   * @param u2nameIndex the index of the name in the constant pool.
   */
  public PackageConstant(int u2nameIndex) {
    this.u2nameIndex = u2nameIndex;
  }

  /** Returns the name. */
  public String getName(Clazz clazz) {
    return clazz.getString(u2nameIndex);
  }

  // Implementations for Constant.

  @Override
  public int getTag() {
    return Constant.PACKAGE;
  }

  @Override
  public boolean isCategory2() {
    return false;
  }

  @Override
  public void accept(Clazz clazz, ConstantVisitor constantVisitor) {
    constantVisitor.visitPackageConstant(clazz, this);
  }

  // Implementations for Object.

  @Override
  public boolean equals(Object object) {
    if (object == null || !this.getClass().equals(object.getClass())) {
      return false;
    }

    if (this == object) {
      return true;
    }

    PackageConstant other = (PackageConstant) object;

    return this.u2nameIndex == other.u2nameIndex;
  }

  @Override
  public int hashCode() {
    return Constant.PACKAGE ^ (u2nameIndex << 5);
  }

  @Override
  public String toString() {
    return "Package(" + u2nameIndex + ")";
  }
}
