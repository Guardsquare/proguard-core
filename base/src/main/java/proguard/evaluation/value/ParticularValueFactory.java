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

import org.jetbrains.annotations.NotNull;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This class provides methods to create and reuse Value instances that have particular values,
 * whenever they are known.
 */
public class ParticularValueFactory extends BasicValueFactory implements ValueFactory {
  // Shared copies of Value objects, to avoid creating a lot of objects.
  static final IntegerValue INTEGER_VALUE_M1 = new ParticularIntegerValue(-1);
  static final IntegerValue INTEGER_VALUE_0 = new ParticularIntegerValue(0);
  static final IntegerValue INTEGER_VALUE_1 = new ParticularIntegerValue(1);
  static final IntegerValue INTEGER_VALUE_2 = new ParticularIntegerValue(2);
  static final IntegerValue INTEGER_VALUE_3 = new ParticularIntegerValue(3);
  static final IntegerValue INTEGER_VALUE_4 = new ParticularIntegerValue(4);
  static final IntegerValue INTEGER_VALUE_5 = new ParticularIntegerValue(5);
  static final LongValue LONG_VALUE_0 = new ParticularLongValue(0);
  static final LongValue LONG_VALUE_1 = new ParticularLongValue(1);
  static final FloatValue FLOAT_VALUE_0 = new ParticularFloatValue(0.0f);
  static final FloatValue FLOAT_VALUE_1 = new ParticularFloatValue(1.0f);
  static final FloatValue FLOAT_VALUE_2 = new ParticularFloatValue(2.0f);
  static final DoubleValue DOUBLE_VALUE_0 = new ParticularDoubleValue(0.0);
  static final DoubleValue DOUBLE_VALUE_1 = new ParticularDoubleValue(1.0);

  private static final int POS_ZERO_FLOAT_BITS = Float.floatToIntBits(0.0f);
  private static final long POS_ZERO_DOUBLE_BITS = Double.doubleToLongBits(0.0);

  private final ValueFactory arrayReferenceValueFactory;
  // This is protected to be usable directly by children, which should ideally be not allowed, but
  // this is a hacky fix for improper inheritance currently taking place (i.e.,
  // DetailedArrayReferenceValue inheriting from IdentifiedReferenceValue, which inherits from this
  // class).
  protected final ValueFactory referenceValueFactory;

  /** Creates a new ParticularValueFactory which does not keep track of particular references. */
  public ParticularValueFactory() {
    this(new ArrayReferenceValueFactory(), new TypedReferenceValueFactory());
  }

  /**
   * Creates a new ParticularValueFactory, which uses the given valuefactory for both array and
   * non-array reference construction.
   */
  public ParticularValueFactory(ValueFactory referenceValueFactory) {
    this(referenceValueFactory, referenceValueFactory);
  }

  /**
   * Creates a new ParticularValueFactory.
   *
   * @param arrayReferenceValueFactory the valuefactory to delegate new array references to.
   * @param referenceValueFactory the valuefactory to delegate new references to.
   */
  public ParticularValueFactory(
      ValueFactory arrayReferenceValueFactory, ValueFactory referenceValueFactory) {
    this.arrayReferenceValueFactory = arrayReferenceValueFactory;
    this.referenceValueFactory = referenceValueFactory;
  }

  // Implementations for ValueFactory.

  @Override
  public IntegerValue createIntegerValue(int value) {
    switch (value) {
      case -1:
        return INTEGER_VALUE_M1;
      case 0:
        return INTEGER_VALUE_0;
      case 1:
        return INTEGER_VALUE_1;
      case 2:
        return INTEGER_VALUE_2;
      case 3:
        return INTEGER_VALUE_3;
      case 4:
        return INTEGER_VALUE_4;
      case 5:
        return INTEGER_VALUE_5;
      default:
        return new ParticularIntegerValue(value);
    }
  }

  @Override
  public LongValue createLongValue(long value) {
    return value == 0L ? LONG_VALUE_0 : value == 1L ? LONG_VALUE_1 : new ParticularLongValue(value);
  }

  @Override
  public FloatValue createFloatValue(float value) {
    // Make sure to distinguish between +0.0 and -0.0.
    return value == 0.0f && Float.floatToIntBits(value) == POS_ZERO_FLOAT_BITS
        ? FLOAT_VALUE_0
        : value == 1.0f
            ? FLOAT_VALUE_1
            : value == 2.0f ? FLOAT_VALUE_2 : new ParticularFloatValue(value);
  }

  @Override
  public DoubleValue createDoubleValue(double value) {
    // Make sure to distinguish between +0.0 and -0.0.
    return value == 0.0 && Double.doubleToLongBits(value) == POS_ZERO_DOUBLE_BITS
        ? DOUBLE_VALUE_0
        : value == 1.0 ? DOUBLE_VALUE_1 : new ParticularDoubleValue(value);
  }

  @Override
  public ReferenceValue createReferenceValue() {
    return referenceValueFactory.createReferenceValue();
  }

  @Override
  public ReferenceValue createReferenceValueNull() {
    return referenceValueFactory.createReferenceValueNull();
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull) {
    return referenceValueFactory.createReferenceValue(
        type, referencedClass, mayBeExtension, mayBeNull);
  }

  /**
   * Deprecated, use {@link ParticularValueFactory#createReferenceValue(Clazz, boolean, boolean,
   * AnalyzedObject)}.
   */
  @Override
  @Deprecated
  public ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object value) {
    return referenceValueFactory.createReferenceValue(
        type, referencedClass, mayBeExtension, mayBeNull, value);
  }

  @Override
  public ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    return referenceValueFactory.createReferenceValue(
        referencedClass, mayBeExtension, mayBeNull, value);
  }

  /**
   * Deprecated, use {@link ParticularValueFactory#createReferenceValue(String, Clazz, boolean,
   * boolean, CodeLocation)}
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
      int creationOffset) {
    return referenceValueFactory.createReferenceValue(
        type,
        referencedClass,
        mayBeExtension,
        mayBeNull,
        creationClass,
        creationMethod,
        creationOffset);
  }

  @Override
  public ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      CodeLocation creationLocation) {
    checkCreationLocation(creationLocation);
    return referenceValueFactory.createReferenceValue(
        type, referencedClass, mayBeExtension, mayBeNull, creationLocation);
  }

  /**
   * Deprecated, use {@link ParticularValueFactory#createReferenceValue(Clazz, boolean, boolean,
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
    return referenceValueFactory.createReferenceValue(
        type,
        referencedClass,
        mayBeExtension,
        mayBeNull,
        creationClass,
        creationMethod,
        creationOffset,
        value);
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
    return referenceValueFactory.createReferenceValue(
        referencedClass, mayBeExtension, mayBeNull, creationLocation, value);
  }

  /**
   * Deprecated, use {@link ParticularValueFactory#createReferenceValueForId(Clazz, boolean,
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
    return referenceValueFactory.createReferenceValueForId(
        type, referencedClass, mayBeExtension, mayBeNull, id, value);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean mayBeNull,
      Object id,
      @NotNull AnalyzedObject value) {
    checkReferenceValue(value);
    return referenceValueFactory.createReferenceValueForId(
        referencedClass, mayBeExtension, mayBeNull, id, value);
  }

  @Override
  public ReferenceValue createReferenceValueForId(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull, Object id) {
    return referenceValueFactory.createReferenceValueForId(
        type, referencedClass, mayBeExtension, mayBeNull, id);
  }

  @Override
  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength) {
    return arrayReferenceValueFactory.createArrayReferenceValue(type, referencedClass, arrayLength);
  }

  @Override
  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength, Object elementValues) {
    return arrayReferenceValueFactory.createArrayReferenceValue(
        type, referencedClass, arrayLength, elementValues);
  }
}
