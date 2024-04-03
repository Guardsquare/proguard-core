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
import java.util.List;
import java.util.Optional;
import proguard.classfile.JavaConstants;
import proguard.classfile.JavaTypeConstants;
import proguard.classfile.MethodDescriptor;
import proguard.classfile.TypeConstants;
import proguard.classfile.util.ClassUtil;
import proguard.evaluation.MethodResult;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.executor.instancehandler.ExecutorInstanceHandler;
import proguard.evaluation.value.DetailedArrayReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This {@link Executor} provides an implementation for {@link Executor#getMethodResult} which tries
 * to resolve the method at runtime and execute it using Java's reflection API {@link
 * java.lang.reflect}.
 */
public abstract class ReflectionExecutor implements Executor {

  @Override
  public MethodResult getMethodResult(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {

    // If reflection is not possible create a result with no particular value, but possibly some
    // extra information on the object identifier
    Optional<MethodResult> fallbackResultOptional =
        createFallbackResultIfInvalidParameters(methodExecutionInfo, valueCalculator);
    if (fallbackResultOptional.isPresent()) {
      return fallbackResultOptional.get();
    }

    ReflectionParameters reflectionParameters =
        new ReflectionParameters(
            methodExecutionInfo.getSignature().descriptor, methodExecutionInfo.getParameters());

    if (methodExecutionInfo.isConstructor()) {
      return executeConstructor(methodExecutionInfo, valueCalculator, reflectionParameters);
    } else {
      return executeMethod(methodExecutionInfo, valueCalculator, reflectionParameters);
    }
  }

  /**
   * Creates a fallback result if the parameters are invalid for reflection execution (i.e., all the
   * parameters should have a known precise value to be able to invoke using reflection with them).
   *
   * <p>Since the executor might be able to provide additional information compared to the fallback
   * of {@link proguard.evaluation.ExecutingInvocationUnit} (e.g., it might know which methods
   * return the same object as their calling instance) in situation in which the error is
   * recoverable this does not return {@link MethodResult#invalidResult()}.
   *
   * <p>If the optional is empty the parameters are valid.
   */
  private Optional<MethodResult> createFallbackResultIfInvalidParameters(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {

    ReferenceValue instance = methodExecutionInfo.getInstanceOrNullIfStatic();

    if (!methodExecutionInfo.isStatic() && (instance == null || !instance.isSpecific())) {
      // Instance must at least be specific (i.e., identified) for non-static methods.
      return Optional.of(MethodResult.invalidResult());
    }

    if (instance != null && instance.isParticular() && instance.getValue().isModeled()) {
      // The instance must be known to be able to invoke on it via reflection.
      return Optional.of(createFallbackResultMethod(methodExecutionInfo, valueCalculator));
    }

    int paramOffset = methodExecutionInfo.isStatic() ? 0 : 1;
    if ((methodExecutionInfo.isInstanceMethod()
            && (!instance.isParticular() // NOSONAR instance can't be null for instance methods
                || isNonPreciseParticularValue(instance)))
        || methodExecutionInfo.getParameters().stream()
            .skip(paramOffset)
            .anyMatch(value -> !value.isParticular() || isNonPreciseParticularValue(value))) {
      // All parameters must be particular and real objects to use reflection. Detailed arrays can
      // be converted to a real object if they are particular.
      if (methodExecutionInfo.isConstructor()) {
        return Optional.of(createFallbackResultConstructor(methodExecutionInfo, valueCalculator));
      } else {
        return Optional.of(createFallbackResultMethod(methodExecutionInfo, valueCalculator));
      }
    }

    return Optional.empty();
  }

  /**
   * Particular primitive types are always precise. For now this has a workaround for {@link
   * DetailedArrayReferenceValue} since the {@link AnalyzedObject} they wrap is not returning true
   * for {@link AnalyzedObject#isPrecise()}.
   */
  private static boolean isNonPreciseParticularValue(Value value) {
    if (!value.isParticular()) {
      throw new IllegalStateException("This method should not be called for non particular values");
    }
    return (value instanceof ReferenceValue)
        && !value.referenceValue().getValue().isPrecise()
        && !(value instanceof DetailedArrayReferenceValue);
  }

  private MethodResult executeMethod(
      MethodExecutionInfo methodExecutionInfo,
      ValueCalculator valueCalculator,
      ReflectionParameters reflectionParameters) {
    Object newReferenceId;
    try {
      Class<?> baseClass =
          Class.forName(
              ClassUtil.externalClassName(methodExecutionInfo.getSignature().getClassName()));

      Object callingInstance = null;
      boolean isCallingInstanceMutable = false;

      if (!methodExecutionInfo.isStatic()) {
        Optional<InstanceCopyResult> objectInstanceResultOptional =
            getInstanceOrCopyIfMutable(methodExecutionInfo.getSpecificInstance());
        if (!objectInstanceResultOptional.isPresent()) {
          return MethodResult.invalidResult();
        }
        callingInstance = objectInstanceResultOptional.get().getInstance().getPreciseValue();
        isCallingInstanceMutable = objectInstanceResultOptional.get().isMutable();
      }

      MethodResult.Builder resultBuilder = new MethodResult.Builder();

      // Try to resolve the method via reflection and invoke the method.
      Object returnResult =
          baseClass
              .getMethod(methodExecutionInfo.getSignature().method, reflectionParameters.classes)
              .invoke(callingInstance, reflectionParameters.objects);

      // The new reference id is the instance one if the method returned the instance
      newReferenceId = null;
      if (!methodExecutionInfo.isStatic() && returnResult == callingInstance) {
        newReferenceId = methodExecutionInfo.getSpecificInstance().id; // NOSONAR can't throw a NPE
      }

      if (methodExecutionInfo.getReturnType().charAt(0) != VOID) {
        Value returnValue =
            valueCalculator.apply(
                methodExecutionInfo.getReturnType(),
                methodExecutionInfo.getReturnClass(),
                true,
                returnResult,
                ClassUtil.isExtendable(methodExecutionInfo.getReturnClass()),
                newReferenceId);
        resultBuilder.setReturnValue(returnValue);
        if (isCallingInstanceMutable && newReferenceId != null) {
          resultBuilder.setUpdatedInstance(returnValue.referenceValue());
        }
      }

      return resultBuilder.build();

    } catch (ClassNotFoundException
        | NoSuchMethodException
        | SecurityException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      return MethodResult.invalidResult();
    }
  }

  private MethodResult executeConstructor(
      MethodExecutionInfo methodExecutionInfo,
      ValueCalculator valueCalculator,
      ReflectionParameters reflectionParameters) {
    try {
      Class<?> baseClass =
          Class.forName(
              ClassUtil.externalClassName(methodExecutionInfo.getSignature().getClassName()));

      Object newInstance =
          baseClass
              .getConstructor(reflectionParameters.classes)
              .newInstance(reflectionParameters.objects);

      // Try to resolve the constructor reflectively and create a new instance.
      return new MethodResult.Builder()
          .setUpdatedInstance(
              valueCalculator
                  .apply(
                      methodExecutionInfo.getTargetType(),
                      methodExecutionInfo.getTargetClass(),
                      true,
                      newInstance,
                      false,
                      methodExecutionInfo.getSpecificInstance().id)
                  .referenceValue())
          .build();
    } catch (ClassNotFoundException
        | NoSuchMethodException
        | SecurityException
        | InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      return MethodResult.invalidResult();
    }
  }

  private MethodResult createFallbackResultMethod(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    MethodResult.Builder builder = new MethodResult.Builder();

    if (methodExecutionInfo.getReturnType().charAt(0) == VOID) {
      return builder.build();
    }

    Object newReferenceId = null;

    if ((methodExecutionInfo.returnsSameTypeAsInstance()
        && getDefaultInstanceHandler()
            .returnsOwnInstance(
                methodExecutionInfo.getSignature().getClassName(),
                methodExecutionInfo.getSignature().method))) {
      newReferenceId = methodExecutionInfo.getSpecificInstance().id;
    }

    builder.setReturnValue(
        valueCalculator.apply(
            methodExecutionInfo.getReturnType(),
            methodExecutionInfo.getReturnClass(),
            false,
            null,
            ClassUtil.isExtendable(methodExecutionInfo.getReturnClass()),
            newReferenceId));

    return builder.build();
  }

  private MethodResult createFallbackResultConstructor(
      MethodExecutionInfo methodExecutionInfo, ValueCalculator valueCalculator) {
    MethodResult.Builder builder = new MethodResult.Builder();
    builder.setUpdatedInstance(
        valueCalculator
            .apply(
                methodExecutionInfo.getTargetType(),
                methodExecutionInfo.getTargetClass(),
                false,
                null,
                false,
                methodExecutionInfo.getSpecificInstance().id)
            .referenceValue());
    return builder.build();
  }

  /**
   * Get an object which will act as the calling instance. If we know that executing the method does
   * not modify the instance this can just be the same value. Otherwise, a copy should be returned
   * in order to not change the reference whose state may be of interest at the end of an analysis.
   *
   * @param instanceValue The {@link ReferenceValue} of the instance.
   * @return The new calling instance.
   */
  protected abstract Optional<InstanceCopyResult> getInstanceOrCopyIfMutable(
      ReferenceValue instanceValue);

  /**
   * Provides a default instance handler used by the executor in case the reflective execution
   * fails.
   *
   * <p>The handler carries information on whether a method returns the same object as its instance.
   *
   * @return the default instance handler.
   */
  protected abstract ExecutorInstanceHandler getDefaultInstanceHandler();

  public static class InstanceCopyResult {
    private final AnalyzedObject instance;
    private final boolean isMutable;

    public InstanceCopyResult(AnalyzedObject instance, boolean isMutable) {
      this.instance = instance;
      this.isMutable = isMutable;
    }

    public AnalyzedObject getInstance() {
      return instance;
    }

    public boolean isMutable() {
      return isMutable;
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
     * @param nonInstanceParameters An array of the non instance parameters of the method.
     */
    public ReflectionParameters(MethodDescriptor descriptor, List<Value> nonInstanceParameters)
        throws IllegalArgumentException {
      int len = nonInstanceParameters.size();
      if (descriptor.getArgumentTypes().size() != len) {
        throw new IllegalArgumentException("Parameter count does not match the method descriptor.");
      }

      objects = new Object[len];
      classes = new Class<?>[len];
      for (int index = 0; index < len; index++) {
        String internalType = descriptor.getArgumentTypes().get(index);
        Value parameter = nonInstanceParameters.get(index);

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
