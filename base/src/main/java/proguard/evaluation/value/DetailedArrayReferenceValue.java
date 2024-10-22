/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.Clazz;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.exception.ArrayIndexOutOfBounds;
import proguard.evaluation.value.object.AnalyzedObject;
import proguard.evaluation.value.object.AnalyzedObjectFactory;
import proguard.util.ArrayUtil;

/**
 * This {@link IdentifiedArrayReferenceValue} represents an identified array reference value with
 * its elements.
 */
public class DetailedArrayReferenceValue extends IdentifiedArrayReferenceValue {
  private static final int MAXIMUM_STORED_ARRAY_LENGTH = 64;

  @NotNull private Value[] values;

  /** Creates a new array reference value with the given ID. */
  private DetailedArrayReferenceValue(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      IntegerValue arrayLength,
      ValueFactory valuefactory,
      int id) {
    super(type, referencedClass, mayBeExtension, arrayLength, valuefactory, id);

    // Initialize the values of the array.
    InitialValueFactory initialValueFactory = new InitialValueFactory(valuefactory);

    String elementType = ClassUtil.isInternalArrayType(type) ? type.substring(1) : type;

    this.values = new Value[arrayLength.value()];

    for (int index = 0; index < values.length; index++) {
      values[index] = initialValueFactory.createValue(elementType);
    }
  }

  /** Deprecated, use {@link DetailedArrayReferenceValue#getValue()}. */
  @Override
  @Deprecated
  public Object value() {
    return values;
  }

  @Override
  public @NotNull AnalyzedObject getValue() {
    return AnalyzedObjectFactory.createDetailedArray(values, type);
  }

  // Implementations for ReferenceValue.

  public IntegerValue integerArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    Value value = arrayLoad(indexValue, valueFactory);
    return value != null ? value.integerValue() : super.integerArrayLoad(indexValue, valueFactory);
  }

  public LongValue longArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    Value value = arrayLoad(indexValue, valueFactory);
    return value != null ? value.longValue() : super.longArrayLoad(indexValue, valueFactory);
  }

  public FloatValue floatArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    Value value = arrayLoad(indexValue, valueFactory);
    return value != null ? value.floatValue() : super.floatArrayLoad(indexValue, valueFactory);
  }

  public DoubleValue doubleArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    Value value = arrayLoad(indexValue, valueFactory);
    return value != null ? value.doubleValue() : super.doubleArrayLoad(indexValue, valueFactory);
  }

  public ReferenceValue referenceArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    Value value = arrayLoad(indexValue, valueFactory);
    return value != null
        ? value.referenceValue()
        : super.referenceArrayLoad(indexValue, valueFactory);
  }

  /** Returns the specified untyped value from the given array, or null if it is unknown. */
  private Value arrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    if (values != null && indexValue.isParticular()) {
      int index = indexValue.value();
      if (index < 0 || index >= values.length) {
        throw new ArrayIndexOutOfBounds(index, values.length);
      }

      return values[index];
    }

    return null;
  }

  public void arrayStore(IntegerValue indexValue, Value value) {
    super.arrayStore(indexValue, value);
    if (values != null) {
      if (indexValue.isParticular()) {
        int index = indexValue.value();
        if (index < 0 || index >= values.length) {
          throw new ArrayIndexOutOfBounds(index, values.length);
        }

        values[index] = value;
      } else {
        for (int index = 0; index < values.length; index++) {
          values[index].generalize(value);
        }
      }
    }
  }

  // Implementations of binary methods of ReferenceValue.

  public ReferenceValue generalize(ReferenceValue other) {
    return other.generalize(this);
  }

  public int equal(ReferenceValue other) {
    return other.equal(this);
  }

  // Implementations for Value.

  public boolean isParticular() {
    if (values == null) {
      return false;
    }

    for (int index = 0; index < values.length; index++) {
      if (!values[index].isParticular()) {
        return false;
      }
    }

    return true;
  }

  /** Creates a deep copy of the DetailedArrayReferenceValue. */
  @Override
  public DetailedArrayReferenceValue copyIfMutable() {
    DetailedArrayReferenceValue copy =
        new DetailedArrayReferenceValue(
            type, referencedClass, mayBeExtension, arrayLength, valuefactory, id);
    if (values != null) {
      copy.values = new Value[values.length];
      for (int i = 0; i < values.length; i++) {
        copy.values[i] = values[i].copyIfMutable();
      }
    }
    return copy;
  }

  // Implementations for Object.

  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (!super.equals(object)) {
      return false;
    }

    DetailedArrayReferenceValue other = (DetailedArrayReferenceValue) object;

    return ArrayUtil.equalOrNull(this.values, other.values);
  }

  public int hashCode() {
    return super.hashCode() ^ ArrayUtil.hashCodeOrNull(values);
  }

  public String toString() {
    if (values == null) {
      return super.toString();
    }

    StringBuffer buffer = new StringBuffer(super.toString());

    buffer.append('{');
    for (int index = 0; index < values.length; index++) {
      buffer.append(values[index]);
      buffer.append(index < values.length - 1 ? ',' : '}');
    }

    return buffer.toString();
  }

  /**
   * If possible it will create a new array reference value with the given ID, otherwise an empty
   * optional is returned
   */
  public static Optional<DetailedArrayReferenceValue> create(
      String type,
      Clazz referencedClass,
      boolean mayBeExtension,
      IntegerValue arrayLength,
      ValueFactory valuefactory,
      int id) {
    // Is the array short enough to analyze?
    if (arrayLength.isParticular()
        && arrayLength.value() >= 0
        && arrayLength.value() <= MAXIMUM_STORED_ARRAY_LENGTH) {
      return Optional.of(
          new DetailedArrayReferenceValue(
              type, referencedClass, mayBeExtension, arrayLength, valuefactory, id));
    }
    return Optional.empty();
  }
}
