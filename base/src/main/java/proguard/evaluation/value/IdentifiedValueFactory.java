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
package proguard.evaluation.value;

import java.util.concurrent.atomic.AtomicInteger;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.*;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This class provides methods to create and reuse Value instances that are identified by unique
 * integer IDs.
 */
public class IdentifiedValueFactory extends ParticularValueFactory {
  private final AtomicInteger integerID = new AtomicInteger(0);
  private final AtomicInteger longID = new AtomicInteger(0);
  private final AtomicInteger floatID = new AtomicInteger(0);
  private final AtomicInteger doubleID = new AtomicInteger(0);
  private static final AtomicInteger referenceIdProvider = new AtomicInteger(0);

  // Implementations for BasicValueFactory.

  public IntegerValue createIntegerValue() {
    return new IdentifiedIntegerValue(this, integerID.incrementAndGet());
  }

  public LongValue createLongValue() {
    return new IdentifiedLongValue(this, longID.incrementAndGet());
  }

  public FloatValue createFloatValue() {
    return new IdentifiedFloatValue(this, floatID.incrementAndGet());
  }

  public DoubleValue createDoubleValue() {
    return new IdentifiedDoubleValue(this, doubleID.incrementAndGet());
  }

  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull) {
    return type == null
        ? TypedReferenceValueFactory.REFERENCE_VALUE_NULL
        : new IdentifiedReferenceValue(
            type, referencedClass, mayBeExtension, mayBeNull, this, generateReferenceId());
  }

  /**
   * Deprecated, use {@link IdentifiedValueFactory#createReferenceValue(Clazz, boolean, boolean,
   * AnalyzedObject)}.
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object value) {
    return this.createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset) {
    return this.createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  /**
   * Deprecated, use {@link IdentifiedValueFactory#createReferenceValue(Clazz, boolean, boolean,
   * CodeLocation, AnalyzedObject)}
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset,
      Object value) {
    return this.createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object id) {
    return type == null
        ? TypedReferenceValueFactory.REFERENCE_VALUE_NULL
        : new IdentifiedReferenceValue(type, referencedClass, mayBeExtension, mayBeNull, this, id);
  }

  /**
   * Deprecated, use {@link IdentifiedValueFactory#createReferenceValueForId(Clazz, boolean,
   * boolean, Object, AnalyzedObject)}.
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValueForId(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Object id,
      Object value) {
    return type == null
        ? TypedReferenceValueFactory.REFERENCE_VALUE_NULL
        : new IdentifiedReferenceValue(type, referencedClass, mayBeExtension, mayBeNull, this, id);
  }

  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength) {
    return type == null
        ? TypedReferenceValueFactory.REFERENCE_VALUE_NULL
        : new IdentifiedArrayReferenceValue(
            TypeConstants.ARRAY + type,
            referencedClass,
            false,
            arrayLength,
            this,
            generateReferenceId());
  }

  public static int generateReferenceId() {
    return referenceIdProvider.incrementAndGet();
  }
}
