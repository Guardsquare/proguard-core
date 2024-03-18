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

package proguard.evaluation.executor;

import static proguard.classfile.TypeConstants.VOID;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import proguard.classfile.JavaConstants;
import proguard.classfile.JavaTypeConstants;
import proguard.classfile.MethodDescriptor;
import proguard.classfile.TypeConstants;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.value.DetailedArrayReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This {@link Executor} provides an implementation for {@link Executor#getMethodResult} which tries
 * to resolve the method at runtime and execute it using Java's reflection API {@link
 * java.lang.reflect}.
 */
public abstract class ReflectionExecutor extends Executor {
  @Override
  public Optional<Value> getMethodResult(
      MethodExecutionInfo methodInfo,
      @Nullable ReferenceValue instance,
      @Nullable AnalyzedObject callingInstanceObject,
      Value[] parameters,
      Function<Object, Value> valueCalculator) {
    if (!methodInfo.isStatic() && (instance == null || !instance.isSpecific())) {
      // Instance must at least be specific.
      return Optional.empty();
    }

    if (callingInstanceObject != null && callingInstanceObject.isModeled()) {
      throw new IllegalStateException(
          "Should not use reflection on a modeled value, are you sure you are matching the expected executor?");
    }

    int paramOffset = methodInfo.isStatic() ? 0 : 1;
    if (!Arrays.stream(parameters)
        .skip(paramOffset)
        .allMatch(
            value ->
                value.isParticular()
                    && (!(value instanceof ReferenceValue)
                        || !value.referenceValue().getValue().isModeled()
                        || value instanceof DetailedArrayReferenceValue))) {
      // All parameters must be particular and real objects to use reflection. Detailed arrays can
      // be converted to a real object if they are particular.
      return Optional.empty();
    }

    ReflectionParameters reflectionParameters =
        new ReflectionParameters(methodInfo.getSignature().descriptor, parameters, paramOffset);

    if (methodInfo.isConstructor()) {
      try {
        Class<?> baseClass =
            Class.forName(ClassUtil.externalClassName(methodInfo.getSignature().getClassName()));

        // Try to resolve the constructor reflectively and create a new instance.
        return Optional.of(
            valueCalculator.apply(
                baseClass
                    .getConstructor(reflectionParameters.classes)
                    .newInstance(reflectionParameters.objects)));
      } catch (ClassNotFoundException
          | NoSuchMethodException
          | SecurityException
          | InstantiationException
          | IllegalAccessException
          | IllegalArgumentException
          | InvocationTargetException e) {
        return Optional.empty();
      }
    } else {
      try {
        Class<?> baseClass =
            Class.forName(ClassUtil.externalClassName(methodInfo.getSignature().getClassName()));
        if ((callingInstanceObject == null || callingInstanceObject.isNull())
            && !methodInfo.isStatic()) {
          throw new IllegalArgumentException();
        }

        Object callingInstance =
            callingInstanceObject == null ? null : callingInstanceObject.getPreciseValue();

        // Try to resolve the method via reflection and invoke the method.
        Object result =
            baseClass
                .getMethod(methodInfo.getSignature().method, reflectionParameters.classes)
                .invoke(callingInstance, reflectionParameters.objects);

        if (methodInfo.getResultType().charAt(0) == VOID) {
          return Optional.empty();
        }

        return Optional.of(valueCalculator.apply(result));

      } catch (ClassNotFoundException
          | NoSuchMethodException
          | SecurityException
          | IllegalAccessException
          | IllegalArgumentException
          | InvocationTargetException e) {
        return Optional.empty();
      }
    }
  }

  /**
   * This class represents the parameters needed for invoking a method using Java's reflection API.
   * It is capable of parsing these parameters arrays of {@link Value}s.
   */
  private static class ReflectionParameters {
    private final Object[] objects;
    private final Class<?>[] classes;

    /**
     * Parse information on a method call into the parameters needed for calling the method via
     * reflection.
     *
     * @param descriptor The descriptor of the method.
     * @param parameters An array of the parameters of the method.
     * @param paramOffset 0 if the method is static, otherwise 1.
     */
    public ReflectionParameters(MethodDescriptor descriptor, Value[] parameters, int paramOffset)
        throws IllegalArgumentException {
      int len = parameters.length - paramOffset;
      if (descriptor.getArgumentTypes().size() != len) {
        throw new IllegalArgumentException("Parameter count does not match the method descriptor.");
      }

      objects = new Object[len];
      classes = new Class<?>[len];
      for (int index = 0; index < len; index++) {
        String internalType = descriptor.getArgumentTypes().get(index);
        Value parameter = parameters[index + paramOffset];

        if (!ClassUtil.isInternalArrayType(internalType)) {
          Class<?> cls;
          try {
            cls = getSingleClass(internalType);
          } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Descriptor type refers to an unknown class.");
          }

          classes[index] = cls;
          objects[index] = getSingleObject(cls, parameter);
        } else {
          String innerType = ClassUtil.internalTypeFromArrayType(internalType);
          if (ClassUtil.isInternalArrayType(innerType)) {
            // unreachable because of DetailedArrayValues not supporting >1D, therefore not being
            // particular
            throw new IllegalArgumentException("Only 1D arrays are supported.");
          }

          Value[] valuesArray = (Value[]) parameter.referenceValue().value();
          Class<?> innerClass;
          try {
            innerClass = getSingleClass(innerType);
          } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Descriptor type refers to an unknown class.");
          }

          Object arrayObject = getArrayObject(innerClass, valuesArray);
          classes[index] =
              arrayObject == null
                  ? Array.newInstance(innerClass, 0).getClass()
                  : arrayObject.getClass();
          objects[index] = arrayObject;
        }
      }
    }

    private static Class<?> getSingleClass(String type) throws ClassNotFoundException {
      switch (type.charAt(0)) {
        case TypeConstants.VOID:
          return void.class;
        case TypeConstants.BOOLEAN:
          return boolean.class;
        case TypeConstants.BYTE:
          return byte.class;
        case TypeConstants.CHAR:
          return char.class;
        case TypeConstants.SHORT:
          return short.class;
        case TypeConstants.INT:
          return int.class;
        case TypeConstants.LONG:
          return long.class;
        case TypeConstants.FLOAT:
          return float.class;
        case TypeConstants.DOUBLE:
          return double.class;
        case TypeConstants.CLASS_START:
          String internalClass = ClassUtil.internalClassNameFromClassType(type);
          return Class.forName(ClassUtil.externalClassName(internalClass));
        default:
          throw new ClassNotFoundException();
      }
    }

    private static int getIntValue(Value value) {
      return value.integerValue().value();
    }

    private static char getCharValue(Value value) {
      return (char) value.integerValue().value();
    }

    private static boolean getBooleanValue(Value value) {
      return value.integerValue().value() != 0;
    }

    private static byte getByteValue(Value value) {
      return (byte) value.integerValue().value();
    }

    private static short getShortValue(Value value) {
      return (short) value.integerValue().value();
    }

    /**
     * Extract an object for the particular value of a {@link Value}.
     *
     * @param cls The class of the value determined using the descriptor.
     * @param value The {@link Value} the object is extracted from.
     * @return The extracted value cast to an {@link Object}.
     */
    private static Object getSingleObject(Class<?> cls, Value value) {
      switch (value.computationalType()) {
        case Value.TYPE_INTEGER:
          return (cls == char.class || cls == Character.class)
              ? getCharValue(value)
              : (cls == byte.class || cls == Byte.class)
                  ? getByteValue(value)
                  : (cls == short.class || cls == Short.class)
                      ? getShortValue(value)
                      : (cls == boolean.class || cls == Boolean.class)
                          ? getBooleanValue(value)
                          : getIntValue(value);
        case Value.TYPE_LONG:
          return value.longValue().value();
        case Value.TYPE_FLOAT:
          return value.floatValue().value();
        case Value.TYPE_DOUBLE:
          return value.doubleValue().value();
        case Value.TYPE_REFERENCE:
          return value.referenceValue().value();
        default:
          return null;
      }
    }

    /**
     * Extract an object for the particular values of an array of {@link Value}s.
     *
     * @param cls The class for the inner type of the array.
     * @param values An array of values.
     * @return The extracted array cast to an {@link Object}.
     */
    private static Object getArrayObject(Class<?> cls, Value[] values) {
      if (values == null) {
        return null;
      }

      int length = values.length;
      switch (cls.getName()) {
          // handle arrays of primitive types separately
        case JavaTypeConstants.INT:
        case JavaConstants.TYPE_JAVA_LANG_INTEGER:
          int[] arrayOfIntegers = new int[length];
          for (int index = 0; index < length; index++) {
            arrayOfIntegers[index] = getIntValue(values[index]);
          }
          return arrayOfIntegers;
        case JavaTypeConstants.LONG:
        case JavaConstants.TYPE_JAVA_LANG_LONG:
          long[] arrayOfLongs = new long[length];
          for (int index = 0; index < length; index++) {
            arrayOfLongs[index] = values[index].longValue().value();
          }
          return arrayOfLongs;
        case JavaTypeConstants.CHAR:
        case JavaConstants.TYPE_JAVA_LANG_CHARACTER:
          char[] arrayOfChars = new char[length];
          for (int index = 0; index < length; index++) {
            arrayOfChars[index] = getCharValue(values[index]);
          }
          return arrayOfChars;
        case JavaTypeConstants.BYTE:
        case JavaConstants.TYPE_JAVA_LANG_BYTE:
          byte[] arrayOfBytes = new byte[length];
          for (int index = 0; index < length; index++) {
            arrayOfBytes[index] = getByteValue(values[index]);
          }
          return arrayOfBytes;
        case JavaTypeConstants.SHORT:
        case JavaConstants.TYPE_JAVA_LANG_SHORT:
          short[] arrayOfShorts = new short[length];
          for (int index = 0; index < length; index++) {
            arrayOfShorts[index] = getShortValue(values[index]);
          }
          return arrayOfShorts;
        case JavaTypeConstants.BOOLEAN:
        case JavaConstants.TYPE_JAVA_LANG_BOOLEAN:
          boolean[] arrayOfBooleans = new boolean[length];
          for (int index = 0; index < length; index++) {
            arrayOfBooleans[index] = getBooleanValue(values[index]);
          }
          return arrayOfBooleans;
        case JavaTypeConstants.FLOAT:
        case JavaConstants.TYPE_JAVA_LANG_FLOAT:
          float[] arrayOfFloats = new float[length];
          for (int index = 0; index < length; index++) {
            arrayOfFloats[index] = values[index].floatValue().value();
          }
          return arrayOfFloats;
        case JavaTypeConstants.DOUBLE:
        case JavaConstants.TYPE_JAVA_LANG_DOUBLE:
          double[] arrayOfDoubles = new double[length];
          for (int index = 0; index < length; index++) {
            arrayOfDoubles[index] = values[index].doubleValue().value();
          }
          return arrayOfDoubles;
        default:
          // Array is not of a primitive type, we can create the instance via reflection
          Object[] arrayOfObjects = (Object[]) Array.newInstance(cls, length);
          for (int index = 0; index < length; index++) {
            arrayOfObjects[index] = values[index].referenceValue().value();
          }
          return arrayOfObjects;
      }
    }
  }
}
