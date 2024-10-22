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

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.TypeConstants;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.value.object.AnalyzedObject;
import proguard.evaluation.value.object.AnalyzedObjectFactory;

/**
 * This identified value factory creates array reference values that also represent their elements,
 * in as far as possible.
 */
public class DetailedArrayValueFactory extends IdentifiedValueFactory {

  private static final Logger log = LogManager.getLogger(ReferenceValue.class);

  /** Creates a new DetailedArrayValueFactory, which does not track reference values. */
  @Deprecated
  public DetailedArrayValueFactory() {
    this(new TypedReferenceValueFactory());
  }

  /**
   * Creates a new DetailedArrayValueFactory, which uses the given value factory for non-array
   * reference construction.
   */
  public DetailedArrayValueFactory(ValueFactory referenceValueFactory) {
    // This is quite ugly, but unavoidable without refactoring the value factories hierarchy. Since
    // what's currently going on is that DetailedArrayReferenceValue is a ParticularValueFactory
    // that overrides all the methods where arrayReferenceValueFactory is used, even if we pass an
    // ArrayReferenceValueFactory to super this will never be used.
    super(new UnusableArrayValueFactory(), referenceValueFactory);
  }

  @Override
  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength) {
    if (type == null) {
      return TypedReferenceValueFactory.REFERENCE_VALUE_NULL;
    } else {
      Optional<DetailedArrayReferenceValue> detailedArray =
          DetailedArrayReferenceValue.create(
              TypeConstants.ARRAY + type,
              referencedClass,
              false,
              arrayLength,
              this,
              generateReferenceId());

      if (detailedArray.isPresent()) {
        return detailedArray.get();
      } else {
        return new IdentifiedArrayReferenceValue(
            TypeConstants.ARRAY + type,
            referencedClass,
            false,
            arrayLength,
            this,
            generateReferenceId());
      }
    }
  }

  @Override
  public ReferenceValue createArrayReferenceValue(
      String type, Clazz referencedClass, IntegerValue arrayLength, Object elementValues) {

    if (type == null) return TypedReferenceValueFactory.REFERENCE_VALUE_NULL;

    String arrayType = TypeConstants.ARRAY + type;

    Optional<DetailedArrayReferenceValue> detailedArrayOpt =
        DetailedArrayReferenceValue.create(
            arrayType, referencedClass, false, arrayLength, this, generateReferenceId());

    if (!detailedArrayOpt.isPresent()) {
      return new IdentifiedArrayReferenceValue(
          arrayType, referencedClass, false, arrayLength, this, generateReferenceId());
    }
    if (!elementValues.getClass().isArray()
        || elementValues.getClass().getComponentType().isArray()) {
      throw new IllegalArgumentException(
          "Only one-dimension array type is supported: " + elementValues.getClass());
    }

    DetailedArrayReferenceValue detailedArray = detailedArrayOpt.get();

    switch (arrayType.charAt(1)) // 0 is the array char
    {
      case TypeConstants.BOOLEAN:
        storeBooleanArray(detailedArray, (boolean[]) elementValues);
        break;
      case TypeConstants.BYTE:
        storeByteArray(detailedArray, (byte[]) elementValues);
        break;
      case TypeConstants.CHAR:
        storeCharArray(detailedArray, (char[]) elementValues);
        break;
      case TypeConstants.SHORT:
        storeShortArray(detailedArray, (short[]) elementValues);
        break;
      case TypeConstants.INT:
        storeIntArray(detailedArray, (int[]) elementValues);
        break;
      case TypeConstants.LONG:
        storeLongArray(detailedArray, (long[]) elementValues);
        break;
      case TypeConstants.FLOAT:
        storeFloatArray(detailedArray, (float[]) elementValues);
        break;
      case TypeConstants.DOUBLE:
        storeDoubleArray(detailedArray, (double[]) elementValues);
        break;
      default:
        storeObjectArray(detailedArray, (Object[]) elementValues);
    }

    return detailedArray;
  }

  private void storeBooleanArray(
      DetailedArrayReferenceValue detailedArray, boolean[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i] ? 1 : 0));
    }
  }

  private void storeByteArray(DetailedArrayReferenceValue detailedArray, byte[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
    }
  }

  private void storeCharArray(DetailedArrayReferenceValue detailedArray, char[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
    }
  }

  private void storeShortArray(DetailedArrayReferenceValue detailedArray, short[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
    }
  }

  private void storeIntArray(DetailedArrayReferenceValue detailedArray, int[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
    }
  }

  private void storeLongArray(DetailedArrayReferenceValue detailedArray, long[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createLongValue(elementValues[i]));
    }
  }

  private void storeFloatArray(DetailedArrayReferenceValue detailedArray, float[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createFloatValue(elementValues[i]));
    }
  }

  private void storeDoubleArray(DetailedArrayReferenceValue detailedArray, double[] elementValues) {
    for (int i = 0; i < elementValues.length; i++) {
      detailedArray.arrayStore(createIntegerValue(i), createDoubleValue(elementValues[i]));
    }
  }

  private void storeObjectArray(DetailedArrayReferenceValue detailedArray, Object[] elements) {
    for (int i = 0; i < elements.length; i++) {
      Object element = elements[i];
      Value elementValue;
      if (element == null) {
        elementValue = createReferenceValueNull();
      } else {
        Clazz referencedClass = detailedArray.referencedClass;
        AnalyzedObject object =
            AnalyzedObjectFactory.create(
                element,
                ClassUtil.internalTypeFromClassName(referencedClass.getName()),
                referencedClass);
        // Call on referenceValueFactory instead of "this" to avoid the behavior from
        // IdentifiedValueFactory (from which DetailedArrayValueFactory should probably not
        // inherit).
        elementValue =
            referenceValueFactory.createReferenceValue(
                referencedClass, ClassUtil.isExtendable(referencedClass), false, object);
      }
      detailedArray.arrayStore(createIntegerValue(i), elementValue);
    }
  }

  /**
   * A value factory that should never be used, used as a placeholder for the arrayValueFactory in
   * {@link ParticularValueFactory}, since all the methods calling it should be overridden by {@link
   * DetailedArrayValueFactory}.
   */
  private static class UnusableArrayValueFactory extends ArrayReferenceValueFactory {
    @Override
    public ReferenceValue createArrayReferenceValue(
        String type, Clazz referencedClass, IntegerValue arrayLength) {
      throw new IllegalStateException(
          "This value factory should never be used, DetailedArrayValueFactory should override all methods calling this");
    }

    @Override
    public ReferenceValue createArrayReferenceValue(
        String type, Clazz referencedClass, IntegerValue arrayLength, Object elementValues) {
      throw new IllegalStateException(
          "This value factory should never be used, DetailedArrayValueFactory should override all methods calling this");
    }
  }
}
