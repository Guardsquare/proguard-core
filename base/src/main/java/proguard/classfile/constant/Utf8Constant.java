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

import proguard.classfile.Clazz;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.util.StringUtil;

/**
 * This {@link Constant} represents a UTF-8 constant in the constant pool.
 *
 * @author Eric Lafortune
 */
public class Utf8Constant extends Constant {

  private String string;

  /** Creates an uninitialized Utf8Constant. */
  public Utf8Constant() {}

  /** Creates a Utf8Constant containing the given string. */
  public Utf8Constant(String string) {
    this.string = string;
  }

  /** Initializes the UTF-8 data with an array of bytes. */
  public void setBytes(byte[] bytes) {
    this.string = StringUtil.getString(bytes);
  }

  /** Returns the UTF-8 data as an array of bytes. */
  public byte[] getBytes() {
    return StringUtil.getModifiedUtf8Bytes(string);
  }

  /** Initializes the UTF-8 data with a String. */
  public void setString(String utf8String) {
    this.string = utf8String;
  }

  /** Returns the UTF-8 data as a String. */
  public String getString() {
    return string;
  }

  // Implementations for Constant.

  @Override
  public int getTag() {
    return Constant.UTF8;
  }

  @Override
  public boolean isCategory2() {
    return false;
  }

  @Override
  public void accept(Clazz clazz, ConstantVisitor constantVisitor) {
    constantVisitor.visitUtf8Constant(clazz, this);
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

    Utf8Constant other = (Utf8Constant) object;

    return this.string.equals(other.string);
  }

  @Override
  public int hashCode() {
    return Constant.UTF8 ^ string.hashCode();
  }

  @Override
  public String toString() {
    return "Utf8(" + string + ")";
  }
}
