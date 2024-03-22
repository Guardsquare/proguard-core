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

import static proguard.classfile.util.ClassUtil.isNullOrFinal;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.analysis.datastructure.CodeLocation;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.value.object.AnalyzedObject;

/** This interface provides methods to create {@link Value} instances. */
public interface ValueFactory {
  /**
   * Creates a new Value of the given type. The type must be a fully specified internal type for
   * primitives, classes, or arrays.
   */
  Value createValue(String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull);

  /** Creates a new IntegerValue with an undefined value. */
  IntegerValue createIntegerValue();

  /** Creates a new IntegerValue with a given particular value. */
  IntegerValue createIntegerValue(int value);

  /** Creates a new IntegerValue with a given possible range. */
  IntegerValue createIntegerValue(int min, int max);

  /** Creates a new LongValue with an undefined value. */
  LongValue createLongValue();

  /** Creates a new LongValue with a given particular value. */
  LongValue createLongValue(long value);

  /** Creates a new FloatValue with an undefined value. */
  FloatValue createFloatValue();

  /** Creates a new FloatValue with a given particular value. */
  FloatValue createFloatValue(float value);

  /** Creates a new DoubleValue with an undefined value. */
  DoubleValue createDoubleValue();

  /** Creates a new DoubleValue with a given particular value. */
  DoubleValue createDoubleValue(double value);

  /** Creates a new ReferenceValue of an undefined type. */
  ReferenceValue createReferenceValue();

  /** Creates a new ReferenceValue that represents <code>null</code>. */
  ReferenceValue createReferenceValueNull();

  default ReferenceValue createReferenceValue(Clazz clazz) {
    return createReferenceValue(
        ClassUtil.internalTypeFromClassName(clazz.getName()), clazz, isNullOrFinal(clazz), true);
  }

  /** Deprecated, use {@link ValueFactory#createReferenceValue(Clazz, AnalyzedObject)}. */
  @Deprecated
  default ReferenceValue createReferenceValue(Clazz clazz, Object value) {
    return createReferenceValue(
        ClassUtil.internalTypeFromClassName(clazz.getName()),
        clazz,
        isNullOrFinal(clazz),
        true,
        value);
  }

  default ReferenceValue createReferenceValue(Clazz clazz, @NotNull AnalyzedObject value) {
    Objects.requireNonNull(value, "The object value should not be null");
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValue(clazz, valueContent);
  }

  /**
   * Creates a new ReferenceValue that represents the given type. The type must be an internal class
   * name or an array type. If the type is <code>null</code>, the ReferenceValue represents <code>
   * null</code>.
   */
  ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull);

  /**
   * Creates a new ReferenceValue that represents the given type. The type must be an internal class
   * name or an array type. If the type is <code>null</code>, the ReferenceValue represents <code>
   * null</code>. The object is the actual value of the reference during execution (can be null).
   *
   * <p>Deprecated, use {@link ValueFactory#createReferenceValue(Clazz, boolean, boolean,
   * AnalyzedObject)}.
   */
  @Deprecated
  ReferenceValue createReferenceValue(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean maybeNull, Object value);

  /**
   * Creates a new ReferenceValue that represents the given type. The type must be an internal class
   * name or an array type. If the type is <code>null</code>, the ReferenceValue represents <code>
   * null</code>.
   *
   * <p>The object wrapped by {@link AnalyzedObject} is either the value of the reference during
   * execution or a {@link proguard.evaluation.value.object.Model} of it.
   */
  default ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean maybeNull,
      @NotNull AnalyzedObject value) {
    Objects.requireNonNull(value, "The object value should not be null");
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValue(
        value.getType(), referencedClass, mayBeExtension, maybeNull, valueContent);
  }

  /**
   * Creates a new ReferenceValue that represents the given type, created at the specified code
   * location. The type must be an internal class name or an array type. If the type is <code>null
   * </code>, the ReferenceValue represents <code>null</code>.
   */
  ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean maybeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset);

  /**
   * Creates a new ReferenceValue that represents the given type, created at the specified code
   * location. The type must be an internal class name or an array type. If the type is <code>null
   * </code>, the ReferenceValue represents <code>null</code>. The object is the actual value of the
   * reference during execution (can be null).
   *
   * <p>Deprecated, use {@link ValueFactory#createReferenceValue(Clazz, boolean, boolean,
   * CodeLocation, AnalyzedObject)}
   */
  @Deprecated
  ReferenceValue createReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean maybeNull,
      Clazz creationClass,
      Method creationMethod,
      int creationOffset,
      Object value);

  /**
   * Creates a new ReferenceValue that represents the given type, created at the specified code
   * location. The type must be an internal class name or an array type. If the type is <code>null
   * </code>, the ReferenceValue represents <code>null</code>.
   *
   * <p>The object wrapped by {@link AnalyzedObject} is either the value of the reference during
   * execution or a {@link proguard.evaluation.value.object.Model} of it.
   */
  default ReferenceValue createReferenceValue(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean maybeNull,
      CodeLocation creationLocation,
      @NotNull AnalyzedObject value) {
    Objects.requireNonNull(value, "The object value should not be null");
    if (!(creationLocation.member instanceof Method)) {
      throw new IllegalStateException("The creation location needs to be in a method");
    }
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValue(
        value.getType(),
        referencedClass,
        mayBeExtension,
        maybeNull,
        creationLocation.clazz,
        (Method) creationLocation.member,
        creationLocation.offset,
        valueContent);
  }

  /**
   * Creates a new ReferenceValue that represents the given type with a specified ID. The type must
   * be an internal class name or an array type. If the type is <code>null</code>, the
   * ReferenceValue represents <code>null</code>.
   */
  ReferenceValue createReferenceValueForId(
      String type, Clazz referencedClass, boolean mayBeExtension, boolean maybeNull, Object id);

  /**
   * Creates a new ReferenceValue that represents the given type with a specified ID. The type must
   * be an internal class name or an array type. If the type is <code>null</code>, the
   * ReferenceValue represents <code>null</code>. The object is the actual value of the reference
   * during execution (can be null).
   *
   * <p>Deprecated, use {@link ValueFactory#createReferenceValueForId(Clazz, boolean, boolean,
   * Object, AnalyzedObject)}.
   */
  @Deprecated
  ReferenceValue createReferenceValueForId(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean maybeNull,
      Object id,
      Object value);

  /**
   * Creates a new ReferenceValue that represents the given type with a specified ID. The type must
   * be an internal class name or an array type. If the type is <code>null</code>, the
   * ReferenceValue represents <code>null</code>.
   *
   * <p>The object wrapped by {@link AnalyzedObject} is either the value of the reference during
   * execution or a {@link proguard.evaluation.value.object.Model} of it.
   */
  default ReferenceValue createReferenceValueForId(
      Clazz referencedClass,
      boolean mayBeExtension,
      boolean maybeNull,
      Object id,
      @NotNull AnalyzedObject value) {
    Objects.requireNonNull(value, "The object value should not be null");
    Object valueContent = value.isModeled() ? value.getModeledValue() : value.getPreciseValue();
    return createReferenceValueForId(
        value.getType(), referencedClass, mayBeExtension, maybeNull, id, valueContent);
  }

  /**
   * Creates a new ReferenceValue that represents a non-null array with elements of the given type,
   * with the given length.
   */
  ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength);

  /**
   * Creates a new ReferenceValue that represents a non-null array with elements of the given type,
   * with the given length and initial element values.
   */
  ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength, Object elementValues);
}
