/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2026 Guardsquare NV
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

package proguard.analysis.cpa.jvm.domain.taint;

import proguard.analysis.cpa.domain.taint.TaintSource;
import proguard.classfile.FieldSignature;
import proguard.classfile.Signature;

/**
 * A {@link JvmStaticFieldTaintSource} specifies a static field whose reads ({@code getstatic})
 * taint the value pushed on the stack. It is keyed on the {@link FieldSignature} of the static
 * field.
 */
public class JvmStaticFieldTaintSource implements TaintSource {

  /** The signature of the static field. */
  public final FieldSignature fieldSignature;

  /**
   * Create a static field taint source.
   *
   * @param fieldSignature the signature of the static field
   */
  public JvmStaticFieldTaintSource(FieldSignature fieldSignature) {
    this.fieldSignature = fieldSignature;
  }

  // implementations for TaintSource

  @Override
  public Signature getSignature() {
    return fieldSignature;
  }

  // implementations for Object

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JvmStaticFieldTaintSource)) {
      return false;
    }
    JvmStaticFieldTaintSource other = (JvmStaticFieldTaintSource) obj;
    return fieldSignature.equals(other.fieldSignature);
  }

  @Override
  public int hashCode() {
    return fieldSignature.hashCode();
  }

  @Override
  public String toString() {
    return "[TaintSource] static field read " + fieldSignature.getFqn();
  }
}
