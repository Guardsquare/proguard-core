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

package proguard.evaluation;

import static proguard.classfile.AccessConstants.FINAL;
import static proguard.classfile.AccessConstants.STATIC;
import static proguard.classfile.ClassConstants.TYPE_JAVA_LANG_STRING;
import static proguard.classfile.TypeConstants.BOOLEAN;
import static proguard.classfile.TypeConstants.BYTE;
import static proguard.classfile.TypeConstants.CHAR;
import static proguard.classfile.TypeConstants.DOUBLE;
import static proguard.classfile.TypeConstants.FLOAT;
import static proguard.classfile.TypeConstants.INT;
import static proguard.classfile.TypeConstants.LONG;
import static proguard.classfile.TypeConstants.SHORT;
import static proguard.classfile.TypeConstants.VOID;
import static proguard.classfile.util.ClassUtil.isInternalPrimitiveType;
import static proguard.classfile.util.ClassUtil.isNullOrFinal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.Member;
import proguard.classfile.Method;
import proguard.classfile.MethodDescriptor;
import proguard.classfile.MethodSignature;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.ConstantValueAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.DoubleConstant;
import proguard.classfile.constant.FieldrefConstant;
import proguard.classfile.constant.FloatConstant;
import proguard.classfile.constant.IntegerConstant;
import proguard.classfile.constant.LongConstant;
import proguard.classfile.constant.StringConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.MemberAccessFilter;
import proguard.classfile.visitor.MemberVisitor;
import proguard.evaluation.executor.Executor;
import proguard.evaluation.executor.MethodExecutionInfo;
import proguard.evaluation.executor.StringReflectionExecutor;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;

/**
 * This {@link InvocationUnit} is capable of executing the invoked methods with particular values as
 * parameters.
 *
 * <p>If a method has a return value, this value is determined and supplied as a particular {@link
 * Value}. Methods that operate on an instance are able to modify the calling instance. For such
 * methods, references in the stack and variables to that instance are replaced with the modified
 * one. This also applies to constructors.
 *
 * <p>This class is responsible for creating the final particular {@link Value}s for methods that
 * were handled without errors and creating fallback values in case the method could not be handled.
 * It also performs the action of replacing references of instances that changed in the stack and
 * variables if needed. This is however limited as we only update the instance if it was modified
 * and ignores all other parameters which might have changed too.
 *
 * <p>An {@link ExecutingInvocationUnit} delegates the acquisition of method results and certain
 * other decisions (e.g. whether replacing references in the stack and variables is needed) to its
 * {@link Executor}s. Per default, an instance of this class will have access to a {@link
 * StringReflectionExecutor} that handles methods of {@link String}, {@link StringBuilder} and
 * {@link StringBuffer}. The {@link Builder}, allows for disabling the default {@link
 * StringReflectionExecutor} and adding other {@link Executor}s.
 */
public class ExecutingInvocationUnit extends BasicInvocationUnit {
  @Nullable private Value[] parameters;
  private final boolean enableSameInstanceIdApproximation;
  private final List<Executor> registeredExecutors;

  // Lazily initialized lookup from method signatures to their responsible executor.
  private final Map<MethodSignature, Executor> responsibleExecutor = new HashMap<>();

  /** Creates an {@link ExecutingInvocationUnit}. */
  protected ExecutingInvocationUnit(
      ValueFactory valueFactory,
      boolean enableSameInstanceIdApproximation,
      List<Executor> registeredExecutors) {
    super(valueFactory);
    this.enableSameInstanceIdApproximation = enableSameInstanceIdApproximation;
    this.registeredExecutors = registeredExecutors;
  }

  /** Deprecated constructor, use {@link ExecutingInvocationUnit.Builder}. */
  @Deprecated
  public ExecutingInvocationUnit(ValueFactory valueFactory) {
    this(
        valueFactory,
        false,
        new ArrayList<>(Collections.singletonList(new StringReflectionExecutor())));
  }

  /** Builds an {@link ExecutingInvocationUnit}. */
  public static class Builder {
    protected boolean enableSameInstanceIdApproximation = false;
    protected boolean useDefaultStringReflectionExecutor = true;
    protected List<Executor.Builder<?>> registeredExecutorBuilders = new ArrayList<>();

    /**
     * For methods that are not supported by any executor, decide, whether a method with matching
     * return and instance types should be treated as a method which returns its instance. In such a
     * case, setting this flag to true will result in no new ID being created for the return value.
     *
     * @param enableSameInstanceIdApproximation whether the approximation should be enabled.
     */
    public Builder setEnableSameInstanceIdApproximation(boolean enableSameInstanceIdApproximation) {
      this.enableSameInstanceIdApproximation = enableSameInstanceIdApproximation;
      return this;
    }

    /**
     * Add an {@link Executor} to be considered by the {@link ExecutingInvocationUnit} when trying
     * to analyze a method call.
     *
     * <p>N.B.: If a method is supported by different executors the first one added gets priority.
     * If {@link
     * proguard.evaluation.ExecutingInvocationUnit.Builder#useDefaultStringReflectionExecutor(boolean)}
     * is not set to <code>false</code> the default {@link StringReflectionExecutor} has the highest
     * priority.
     *
     * @param executor A {@link Executor.Builder} of the {@link Executor} to be added.
     */
    public Builder addExecutor(Executor.Builder<?> executor) {
      registeredExecutorBuilders.add(executor);
      return this;
    }

    /**
     * Add multiple {@link Executor}s to be considered by the {@link ExecutingInvocationUnit} when
     * trying to analyze a method call.
     *
     * <p>N.B.: If a method is supported by different executors the first one added gets priority.
     * If {@link
     * proguard.evaluation.ExecutingInvocationUnit.Builder#useDefaultStringReflectionExecutor(boolean)}
     * is not set to <code>false</code> the default {@link StringReflectionExecutor} has the highest
     * priority.
     *
     * @param executors {@link Executor.Builder}s of the {@link Executor}s to be added.
     */
    public Builder addExecutors(Executor.Builder<?>... executors) {
      registeredExecutorBuilders.addAll(Arrays.asList(executors));
      return this;
    }

    /**
     * Set this flag to false if the {@link ExecutingInvocationUnit} should not use {@link
     * StringReflectionExecutor} by default.
     *
     * @param useDefaultStringReflectionExecutor whether a default {@link StringReflectionExecutor}
     *     should be used.
     */
    public Builder useDefaultStringReflectionExecutor(boolean useDefaultStringReflectionExecutor) {
      this.useDefaultStringReflectionExecutor = useDefaultStringReflectionExecutor;
      return this;
    }

    /**
     * Build the {@link ExecutingInvocationUnit} defined by this builder instance.
     *
     * @param valueFactory The {@link ValueFactory} responsible for creating result values.
     * @return The built {@link ExecutingInvocationUnit}
     */
    public ExecutingInvocationUnit build(ValueFactory valueFactory) {
      List<Executor> registeredExecutors = new ArrayList<>();

      if (useDefaultStringReflectionExecutor)
        registeredExecutors.add(new StringReflectionExecutor.Builder().build());

      registeredExecutorBuilders.stream()
          .map(Executor.Builder::build)
          .forEach(registeredExecutors::add);

      return new ExecutingInvocationUnit(
          valueFactory, enableSameInstanceIdApproximation, registeredExecutors);
    }
  }

  @Override
  public void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant) {
    try {
      super.visitAnyMethodrefConstant(clazz, anyMethodrefConstant);
    } finally {
      this.parameters = null;
    }
  }

  // Overrides for BasicInvocationUnit

  @Override
  public void setMethodParameterValue(
      Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, int parameterIndex, Value value) {
    if (parameters == null) {
      // This is the first invocation of setMethodParameterValue for this method.
      String type = anyMethodrefConstant.getType(clazz);
      int parameterCount = ClassUtil.internalMethodParameterCount(type, isStatic);
      parameters = new Value[parameterCount];
    }
    parameters[parameterIndex] = value;
  }

  @Override
  public boolean methodMayHaveSideEffects(
      Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, String returnType) {
    // Only execute methods which have at least one parameter.
    // If the method is a static method, this means that at least one parameter is set,
    // if the method is an instance call, at least the instance needs to be set.
    // Static calls without parameters will not be called, as the side effects of those cannot
    // currently be tracked.
    return parameters != null && parameters.length > 0;
  }

  @Override
  public Value getMethodReturnValue(
      Clazz clazz, AnyMethodrefConstant anyMethodrefConstant, String returnType) {
    if (anyMethodrefConstant.referencedMethod == null || parameters == null) {
      return super.getMethodReturnValue(clazz, anyMethodrefConstant, returnType);
    }

    MethodExecutionInfo methodInfo = new MethodExecutionInfo(anyMethodrefConstant, null);

    Executor executor = getResponsibleExecutor(methodInfo.getSignature());

    Value result = executeMethod(executor, methodInfo, parameters);
    // Only return the method result if the method is expected to return something.
    return returnType.charAt(0) == VOID ? null : result;
  }

  /**
   * Get the responsible {@link Executor} for a given class name and a method name. Cache previously
   * determined executors.
   *
   * @param signature The method signature.
   * @return The responsible executor.
   */
  private Executor getResponsibleExecutor(MethodSignature signature) {
    return responsibleExecutor.computeIfAbsent(
        signature,
        sig ->
            registeredExecutors.stream()
                .filter(executor -> executor.isSupportedMethodCall(signature))
                .findFirst()
                .orElse(null));
  }

  /**
   * Execute the method given by a {@link ConcreteCall}. See {@link
   * ExecutingInvocationUnit#executeMethod(Executor, MethodExecutionInfo, Value...)}
   *
   * @param call The concrete call.
   * @param parameters The calling parameters.
   * @return The method result value.
   */
  public Value executeMethod(ConcreteCall call, Value... parameters) {
    MethodExecutionInfo methodInfo = new MethodExecutionInfo(call);
    Executor executor = getResponsibleExecutor(call.getTarget());
    return executeMethod(executor, methodInfo, parameters);
  }

  /**
   * Executes a method using a given {@link Executor}. Replace references of the instance in
   * variables and stack if necessary. The return value represents the result of the executed
   * method.
   *
   * @param executor The {@link Executor} which handles this method call.
   * @param methodInfo Information about the method to execute.
   * @param parameters The calling parameters.
   * @return The method result value.
   */
  public Value executeMethod(
      Executor executor, MethodExecutionInfo methodInfo, Value... parameters) {
    ReferenceValue instanceValue = methodInfo.getInstanceValue(parameters).orElse(null);
    Object instanceId = methodInfo.getObjectId(instanceValue).orElse(null);

    boolean returnsSameTypeAsInstance = methodInfo.returnsSameTypeAsInstance(parameters);
    boolean returnsOwnInstance;
    if (executor == null) {
      // The invocation unit is not able to execute the method. Calculate a fallback value.
      returnsOwnInstance =
          // If the method is a constructor, never create a new ID. Otherwise, only keep the ID if
          // return and instance types match and the approximation is enabled.
          methodInfo.isConstructor()
              || enableSameInstanceIdApproximation && returnsSameTypeAsInstance;
      return createFallbackResultValue(methodInfo, returnsOwnInstance, instanceId);
    }

    Object callingInstance = methodInfo.getCallingInstance(executor, parameters).orElse(null);

    // The executor provides further information on whether a method returns its own instance.
    returnsOwnInstance =
        methodInfo.isConstructor()
            || executor.returnsOwnInstance(methodInfo.getSignature(), returnsSameTypeAsInstance);

    // The instantiated object for a constructor or the returned value otherwise
    Value resultValue =
        executor
            .getMethodResult(methodInfo, instanceValue, callingInstance, parameters)
            .map(result -> createResultValue(methodInfo, instanceId, result, returnsOwnInstance))
            .orElse(createFallbackResultValue(methodInfo, returnsOwnInstance, instanceId));

    if (parameters != null && !methodInfo.isStatic() && instanceValue != null) {
      Value oldInstanceValue = parameters[0];
      Value updatedInstanceValue =
          returnsOwnInstance && resultValue != null && resultValue.isParticular()
              // if the method returned its instance there is no need to create a new value
              ? resultValue
              : (!Objects.equals(oldInstanceValue.referenceValue().value(), callingInstance))
                  // otherwise create a value with an updated object
                  ? createUpdatedInstanceValue(instanceValue, instanceId, callingInstance)
                  : null;

      if (updatedInstanceValue != null) {
        if (variables != null) {
          replaceReferenceInVariables(updatedInstanceValue, oldInstanceValue, variables);
        }
        if (stack != null) {
          replaceReferenceOnStack(updatedInstanceValue, oldInstanceValue, stack);
        }
      }
    }

    return resultValue;
  }

  /**
   * Create the result value of a method for a given method result object.
   *
   * @param methodInfo Information about the method.
   * @param instanceId The ID of the instance.
   * @param result The method result provided the {@link Executor}.
   * @param returnsOwnInstance whether the method returns its own instance.
   * @return The result value of the method.
   */
  private Value createResultValue(
      MethodExecutionInfo methodInfo,
      Object instanceId,
      Object result,
      boolean returnsOwnInstance) {
    String resultType = methodInfo.getResultType();
    if (isInternalPrimitiveType(resultType)) {
      switch (resultType.charAt(0)) {
        case BOOLEAN:
          return valueFactory.createIntegerValue(((Boolean) result) ? 1 : 0);
        case CHAR:
          return valueFactory.createIntegerValue((Character) result);
        case BYTE:
          return valueFactory.createIntegerValue((Byte) result);
        case SHORT:
          return valueFactory.createIntegerValue((Short) result);
        case INT:
          return valueFactory.createIntegerValue((Integer) result);
        case FLOAT:
          return valueFactory.createFloatValue((Float) result);
        case DOUBLE:
          return valueFactory.createDoubleValue((Double) result);
        case LONG:
          return valueFactory.createLongValue((Long) result);
      }
      return null; // unreachable
    } else if (resultType.charAt(0) == VOID) {
      return null;
    }

    if (ClassUtil.internalArrayTypeDimensionCount(resultType) == 1
        && isInternalPrimitiveType(ClassUtil.internalTypeFromArrayType(resultType))) {
      return valueFactory.createArrayReferenceValue(
          resultType, null, valueFactory.createIntegerValue(Array.getLength(result)), result);
    }

    boolean resultMayBeNull = result == null;
    boolean resultMayBeExtension = !methodInfo.isConstructor();

    if (instanceId != null && returnsOwnInstance) {
      return valueFactory.createReferenceValueForId(
          resultType,
          methodInfo.getResultClass(),
          resultMayBeExtension,
          resultMayBeNull,
          instanceId,
          result);
    }

    return methodInfo
        .getCaller()
        .map(
            caller ->
                valueFactory.createReferenceValue(
                    resultType,
                    methodInfo.getResultClass(),
                    resultMayBeExtension,
                    resultMayBeNull,
                    caller.clazz,
                    (Method) caller.member,
                    caller.offset,
                    result))
        .orElse(
            valueFactory.createReferenceValue(
                resultType,
                methodInfo.getResultClass(),
                resultMayBeExtension,
                resultMayBeNull,
                result));
  }

  /**
   * Creates {@link Value} for the updated instance.
   *
   * @param instanceValue The old reference value of the instance.
   * @param instanceId The id of the instance value.
   * @param instance The updated object.
   * @return the updated value.
   */
  private Value createUpdatedInstanceValue(
      ReferenceValue instanceValue, Object instanceId, Object instance) {
    return valueFactory.createReferenceValueForId(
        instanceValue.internalType(),
        instanceValue.getReferencedClass(),
        instanceValue.mayBeExtension(),
        instance == null,
        instanceId,
        instance);
  }

  /**
   * Create a fallback result value for the method. Such a value is either used, when no matching
   * executor was available for the method call or the matching executor failed to calculate a
   * result.
   *
   * @param methodInfo Information about the method.
   * @param returnsOwnInstance whether the method returns its own instance.
   * @param instanceId The ID of the instance.
   * @return The fallback result value.
   */
  private Value createFallbackResultValue(
      MethodExecutionInfo methodInfo, boolean returnsOwnInstance, Object instanceId) {
    String resultType = methodInfo.getResultType();
    if (resultType.charAt(0) == VOID) {
      return null;
    }

    if (isInternalPrimitiveType(resultType)) {
      return valueFactory.createValue(resultType, null, false, false);
    }

    Clazz resultClass = methodInfo.getResultClass();
    if (instanceId != null && returnsOwnInstance) {
      return valueFactory.createReferenceValueForId(
          resultType, resultClass, isNullOrFinal(resultClass), true, instanceId);
    }

    return valueFactory.createValue(resultType, resultClass, isNullOrFinal(resultClass), true);
  }

  /**
   * Return whether the invocation unit is expected to handle the method call based on the class
   * name and the method name.
   *
   * @param internalClassName The class name for the method.
   * @param methodName The name of the method.
   * @return whether the invocation unit is expected to handle the method call.
   */
  public boolean isSupportedMethodCall(String internalClassName, String methodName) {
    return isSupportedMethodCall(
        new MethodSignature(internalClassName, methodName, (MethodDescriptor) null));
  }

  public boolean isSupportedMethodCall(MethodSignature methodSignature) {
    return getResponsibleExecutor(methodSignature) != null;
  }

  /**
   * Determines whether the stack/variables need to be updated with the changed instance.
   *
   * @param clazz The class of the method.
   * @param method The method.
   * @param instance The instance of the method call.
   * @return whether the method returns its own instance.
   */
  public boolean returnsOwnInstance(Clazz clazz, Method method, Value instance) {
    if (!(instance instanceof ReferenceValue)) return false;

    MethodExecutionInfo methodInfo = new MethodExecutionInfo(clazz, method, null);
    if (methodInfo.isConstructor()) return true;

    Executor executor = getResponsibleExecutor(methodInfo.getSignature());
    boolean returnsSameTypeAsInstance = methodInfo.returnsSameTypeAsInstance(instance);
    if (executor == null) {
      return methodInfo.isConstructor()
          || enableSameInstanceIdApproximation && methodInfo.returnsSameTypeAsInstance(parameters);
    }

    return executor.returnsOwnInstance(methodInfo.getSignature(), returnsSameTypeAsInstance);
  }

  /**
   * Iterate the variables and replace all occurrences of oldValue with newValue.
   *
   * @param newValue the value which should be put in.
   * @param oldValue the value to replace.
   * @param variables the variables to look through.
   */
  private void replaceReferenceInVariables(Value newValue, Value oldValue, Variables variables) {
    if (!oldValue.isSpecific() || variables == null) return;
    // we need to replace all instances on the stack and in the vars with new instances now.
    for (int i = 0; i < variables.size(); i++) {
      Value value = variables.getValue(i);
      if (Objects.equals(value, oldValue)) {
        variables.store(i, newValue);
      }
      if (value != null && value.isCategory2()) i++;
    }
  }

  /**
   * Iterate the stack and replace all occurrences of oldValue with newValue.
   *
   * @param newValue the value which should be put in.
   * @param oldValue the value to replace.
   * @param stack the stack to look through.
   */
  private void replaceReferenceOnStack(Value newValue, Value oldValue, Stack stack) {
    if (!oldValue.isSpecific()) return;
    for (int i = 0; i < stack.size(); i++) {
      Value top = stack.getTop(i);
      if (Objects.equals(top, oldValue)) {
        stack.setTop(i, newValue);
      }
    }
  }

  @Override
  public Value getFieldValue(Clazz clazz, FieldrefConstant fieldrefConstant, String type) {
    // get values from static final fields
    FieldValueGetterVisitor constantVisitor = new FieldValueGetterVisitor();
    fieldrefConstant.referencedFieldAccept(
        new MemberAccessFilter(STATIC | FINAL, 0, constantVisitor));

    return constantVisitor.value == null
        ? super.getFieldValue(clazz, fieldrefConstant, type)
        : constantVisitor.value;
  }

  private class FieldValueGetterVisitor
      implements MemberVisitor, AttributeVisitor, ConstantVisitor {

    Value value = null;
    private ProgramField currentField;

    // Implementations for MemberVisitor

    @Override
    public void visitAnyMember(Clazz clazz, Member member) {}

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField) {
      this.currentField = programField;
      programField.attributesAccept(programClass, this);
    }

    // Implementations for AttributeVisitor

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitConstantValueAttribute(
        Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute) {
      clazz.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex, this);
    }

    // Implementations for ConstantVisitor

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {}

    @Override
    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant) {
      value = valueFactory.createIntegerValue(integerConstant.getValue());
    }

    @Override
    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant) {
      value = valueFactory.createFloatValue(floatConstant.getValue());
    }

    @Override
    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant) {
      value = valueFactory.createDoubleValue(doubleConstant.getValue());
    }

    @Override
    public void visitLongConstant(Clazz clazz, LongConstant longConstant) {
      value = valueFactory.createLongValue(longConstant.getValue());
    }

    @Override
    public void visitStringConstant(Clazz clazz, StringConstant stringConstant) {
      value =
          valueFactory.createReferenceValue(
              TYPE_JAVA_LANG_STRING,
              currentField.referencedClass,
              isNullOrFinal(currentField.referencedClass),
              false,
              stringConstant.getString(clazz));
    }
  }
}
