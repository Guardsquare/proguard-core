/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.TypeConstants;
import proguard.evaluation.value.object.AnalyzedObject;

/** This class provides methods to create and reuse Value instances. */
public class BasicValueFactory implements ValueFactory {
  // Shared copies of Value objects, to avoid creating a lot of objects.
  public static final UnknownValue UNKNOWN_VALUE = new UnknownValue();
  public static final IntegerValue INTEGER_VALUE = new UnknownIntegerValue();
  public static final LongValue LONG_VALUE = new UnknownLongValue();
  public static final FloatValue FLOAT_VALUE = new UnknownFloatValue();
  public static final DoubleValue DOUBLE_VALUE = new UnknownDoubleValue();
  public static final ReferenceValue REFERENCE_VALUE = new UnknownReferenceValue();

  // Implementations for BasicValueFactory.

  public Value createValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull) {
    switch (type.charAt(0)) {
      case TypeConstants.VOID:
        return null;
      case TypeConstants.BOOLEAN:
      case TypeConstants.BYTE:
      case TypeConstants.CHAR:
      case TypeConstants.SHORT:
      case TypeConstants.INT:
        return createIntegerValue();
      case TypeConstants.LONG:
        return createLongValue();
      case TypeConstants.FLOAT:
        return createFloatValue();
      case TypeConstants.DOUBLE:
        return createDoubleValue();
      default:
        return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
    }
  }

  @Override
  public IntegerValue createIntegerValue() {
    return INTEGER_VALUE;
  }

  @Override
  public IntegerValue createIntegerValue(int value) {
    return createIntegerValue();
  }

  @Override
  public IntegerValue createIntegerValue(int min, int max) {
    return createIntegerValue();
  }

  @Override
  public LongValue createLongValue() {
    return LONG_VALUE;
  }

  @Override
  public LongValue createLongValue(long value) {
    return createLongValue();
  }

  @Override
  public FloatValue createFloatValue() {
    return FLOAT_VALUE;
  }

  @Override
  public FloatValue createFloatValue(float value) {
    return createFloatValue();
  }

  @Override
  public DoubleValue createDoubleValue() {
    return DOUBLE_VALUE;
  }

  @Override
  public DoubleValue createDoubleValue(double value) {
    return createDoubleValue();
  }

  @Override
  public ReferenceValue createReferenceValue() {
    return REFERENCE_VALUE;
  }

  @Override
  public ReferenceValue createReferenceValueNull() {
    return REFERENCE_VALUE;
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull) {
    return createReferenceValue();
  }

  /**
   * Deprecated, use {@link BasicValueFactory#createReferenceValue(Clazz, boolean, boolean,
   * AnalyzedObject)}.
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object value) {
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  @Override
  public ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValue(
        value.getType(), referencedClass, mayBeExtension, mayBeNull, valueContent);
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
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  /**
   * Deprecated, use {@link BasicValueFactory#createReferenceValue(Clazz, boolean, boolean,
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
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  @Override
  public ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      CodeLocation creationLocation,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    checkCreationLocation(creationLocation);
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValue(
        value.getType(),
        referencedClass,
        mayBeExtension,
        mayBeNull,
        creationLocation.clazz,
        (Method) creationLocation.member,
        creationLocation.offset,
        valueContent);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object id) {
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  /**
   * Deprecated, use {@link BasicValueFactory#createReferenceValueForId(Clazz, boolean, boolean,
   * Object, AnalyzedObject)}.
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
    return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Object id,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValueForId(
        value.getType(), referencedClass, mayBeExtension, mayBeNull, id, valueContent);
  }

  @Override
  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength) {
    return createReferenceValue(type, referencedClass, false, false);
  }

  @Override
  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength, Object elementValues) {
    return createArrayReferenceValue(type, referencedClass, arrayLength);
  }

  protected static void checkReferenceValue(AnalyzedObject value) {
    Objects.requireNonNull(value, "The object value should not be null");
  }

  protected static void checkCreationLocation(CodeLocation creationLocation) {
    if (!(creationLocation.member instanceof Method)) {
      throw new IllegalStateException("The creation location needs to be in a method");
    }
  }
}
