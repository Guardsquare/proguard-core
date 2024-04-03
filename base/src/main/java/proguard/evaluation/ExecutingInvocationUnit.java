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
import static proguard.classfile.TypeConstants.BOOLEAN;
import static proguard.classfile.TypeConstants.BYTE;
import static proguard.classfile.TypeConstants.CHAR;
import static proguard.classfile.TypeConstants.DOUBLE;
import static proguard.classfile.TypeConstants.FLOAT;
import static proguard.classfile.TypeConstants.INT;
import static proguard.classfile.TypeConstants.LONG;
import static proguard.classfile.TypeConstants.SHORT;
import static proguard.classfile.TypeConstants.VOID;
import static proguard.classfile.util.ClassUtil.isExtendable;
import static proguard.classfile.util.ClassUtil.isInternalPrimitiveType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.analysis.datastructure.CodeLocation;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.Member;
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
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.ValueFactory;
import proguard.evaluation.value.object.AnalyzedObject;
import proguard.evaluation.value.object.AnalyzedObjectFactory;
import proguard.evaluation.value.object.Model;
import proguard.util.PartialEvaluatorUtils;

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
  private static final Logger log = LogManager.getLogger(ExecutingInvocationUnit.class);
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

      if (useDefaultStringReflectionExecutor) {
        registeredExecutors.add(new StringReflectionExecutor.Builder().build());
      }

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

    MethodExecutionInfo methodInfo =
        new MethodExecutionInfo(anyMethodrefConstant, null, parameters);
    Executor executor = getResponsibleExecutor(methodInfo.getSignature());

    MethodResult result = executeMethod(executor, methodInfo);

    // If side effects happened on any identified value, update stack and variables with them
    applySideEffects(result);

    if (methodInfo.returnsVoid()) {
      return null;
    }

    if (!result.isReturnValuePresent()) {
      throw new IllegalStateException(
          "The return value is not present for a method not returning void");
    }

    return result.getReturnValue();
  }

  private void applySideEffects(MethodResult result) {
    List<Value> valuesWithSideEffects = new ArrayList<>();

    if (result.isInstanceUpdated()) {
      getUpdatedInstance(result).ifPresent(valuesWithSideEffects::add);
    }

    if (result.isAnyParameterUpdated()) {
      valuesWithSideEffects.addAll(getUpdatedParameters(result));
    }

    for (Value replacingValue : valuesWithSideEffects) {
      replaceReferenceInVariables(replacingValue, variables);
      replaceReferenceOnStack(replacingValue, stack);
    }
  }

  private Optional<Value> getUpdatedInstance(MethodResult result) {
    IdentifiedReferenceValue updatedInstance =
        (IdentifiedReferenceValue) result.getUpdatedInstance();
    if (!updatedInstance.isSpecific() || !parameters[0].isSpecific()) {
      throw new IllegalStateException(
          "An updated instance was provided but either it or the original instance are not specific");
    }
    if (!updatedInstance.id.equals(((IdentifiedReferenceValue) parameters[0]).id)) {
      log.error(
          "The updated instance has unexpectedly a different identifier from the calling instance");
      return Optional.empty();
    } else {
      return Optional.of(result.getUpdatedInstance());
    }
  }

  private Collection<Value> getUpdatedParameters(MethodResult result) {
    Collection<Value> toReturn = new ArrayList<>();

    List<Value> updatedParameters = result.getUpdatedParameters();
    int firstParameterIndex = isStatic ? 0 : 1;
    for (int i = 0; i < updatedParameters.size(); i++) {
      Value updatedParameter = updatedParameters.get(i);
      if (updatedParameter != null) {
        Value oldParameter = parameters[i + firstParameterIndex];
        if (!updatedParameter.isSpecific() || !oldParameter.isSpecific()) {
          throw new IllegalStateException(
              "An updated parameter was provided but either it or the original parameter are not specific");
        }
        Object updatedId =
            PartialEvaluatorUtils.getIdFromSpecificReferenceValue(
                updatedParameter.referenceValue());
        Object oldId =
            PartialEvaluatorUtils.getIdFromSpecificReferenceValue(oldParameter.referenceValue());
        if (!oldId.equals(updatedId)) {
          log.error(
              "The updated parameter has unexpectedly a different identifier from its original value");
        } else {
          toReturn.add(updatedParameter);
        }
      }
    }

    return toReturn;
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
   * ExecutingInvocationUnit#executeMethod(Executor, MethodExecutionInfo)}
   *
   * @param call The concrete call.
   * @param parameters The calling parameters.
   * @return The method result value.
   */
  public MethodResult executeMethod(ConcreteCall call, Value... parameters) {
    MethodExecutionInfo methodInfo = new MethodExecutionInfo(call, parameters);
    Executor executor = getResponsibleExecutor(call.getTarget());
    return executeMethod(executor, methodInfo);
  }

  /**
   * Executes a method using a given {@link Executor}. Replace references of the instance in
   * variables and stack if necessary. The return value represents the result of the executed
   * method.
   *
   * @param executor The {@link Executor} which handles this method call.
   * @param methodInfo Information about the method to execute.
   * @return The method result value.
   */
  public MethodResult executeMethod(Executor executor, MethodExecutionInfo methodInfo) {

    if (executor == null) {
      return createFallbackResult(methodInfo);
    }

    MethodResult result =
        executor.getMethodResult(
            methodInfo,
            (type, referencedClazz, isParticular, concreteValue, valueMayBeExtension, valueId) ->
                createValue(
                    type,
                    referencedClazz,
                    isParticular,
                    concreteValue,
                    valueMayBeExtension,
                    valueId,
                    methodInfo.getCaller()));

    if (result.isResultValid()) {
      return result;
    }

    return createFallbackResult(methodInfo);
  }

  /**
   * Provides a result with as much information as possible if an executor is not able to provide
   * it.
   */
  private MethodResult createFallbackResult(MethodExecutionInfo methodInfo) {

    ReferenceValue instanceValue = methodInfo.getInstanceOrNullIfStatic();

    Object instanceId = null;
    if (instanceValue != null && instanceValue.isSpecific()) {
      instanceId = ((IdentifiedReferenceValue) instanceValue).id;
    }

    boolean returnsSameTypeAsInstance = methodInfo.returnsSameTypeAsInstance();
    MethodResult.Builder resultBuilder = new MethodResult.Builder();
    if (methodInfo.returnsVoid()) {
      return resultBuilder.build();
    }
    // The invocation unit is not able to execute the method. Calculate a fallback value.
    boolean returnsOwnInstance =
        // Keep the id if return and instance types match and the approximation is enabled.
        methodInfo.isConstructor()
            || enableSameInstanceIdApproximation && returnsSameTypeAsInstance;
    Object newInstanceId = returnsOwnInstance ? instanceId : null;
    resultBuilder.setReturnValue(
        createNonParticularValue(
            methodInfo.getReturnType(),
            methodInfo.getReturnClass(),
            isExtendable(methodInfo.getReturnClass()),
            newInstanceId));
    return resultBuilder.build();
  }

  /**
   * Create a {@link Value} given all available known information about it (included the actual
   * tracked value or a {@link Model} of it, if available, and its reference identifier if the value
   * has the same reference as an existing one).
   *
   * <p>This method is not limited to particular value and can be used to create any value given the
   * available amount of information.
   *
   * @param type the static type of the created value (runtime type might implement/extend it).
   * @param referencedClass the {@link Clazz} of the value (if it's a reference value).
   * @param isParticular whether the value to create is particular. If not the `concreteValue`
   *     parameter will be ignored.
   * @param concreteValue the value of the tracked object. Can be the actual value or a {@link
   *     Model}.
   * @param valueMayBeExtension whether the created value might actually be an extension of the
   *     type. This should always be false for the instance created by constructors since they
   *     always create an object of the exact type. This should usually be {@link
   *     ClassUtil#isExtendable(Clazz)} for non constructor calls or more precise information if the
   *     caller is able to provide it.
   * @param valueId the already known reference identifier of the created value. Null if the
   *     identifier was not previously known. This is particularly important for constructors, since
   *     they always return void and the only way to associate the constructed object to its
   *     existing references is via the id.
   * @param callerLocation the code location of the caller. This is a completely optional parameter
   *     and the value can be created without it.
   * @return The {@link Value} corresponding to the given parameters.
   */
  private @NotNull Value createValue(
      String type,
      Clazz referencedClass,
      boolean isParticular,
      @Nullable Object concreteValue,
      boolean valueMayBeExtension,
      @Nullable Object valueId,
      @Nullable CodeLocation callerLocation) {

    if (type.charAt(0) == VOID) {
      throw new IllegalStateException("A value should not be created for void type");
    }

    if (!isParticular) {
      return createNonParticularValue(type, referencedClass, valueMayBeExtension, valueId);
    }

    if (ClassUtil.isInternalPrimitiveType(type)) {
      Objects.requireNonNull(concreteValue, "Values of primitive types can't be null");

      switch (type.charAt(0)) {
        case BOOLEAN:
          return valueFactory.createIntegerValue(((Boolean) concreteValue) ? 1 : 0);
        case CHAR:
          return valueFactory.createIntegerValue((Character) concreteValue);
        case BYTE:
          return valueFactory.createIntegerValue((Byte) concreteValue);
        case SHORT:
          return valueFactory.createIntegerValue((Short) concreteValue);
        case INT:
          return valueFactory.createIntegerValue((Integer) concreteValue);
        case FLOAT:
          return valueFactory.createFloatValue((Float) concreteValue);
        case DOUBLE:
          return valueFactory.createDoubleValue((Double) concreteValue);
        case LONG:
          return valueFactory.createLongValue((Long) concreteValue);
        default:
          throw new IllegalStateException("Trying to create a value of an unknown primitive type");
      }
    }

    // TODO: update to handle array of references the same way as primitive arrays
    if (ClassUtil.internalArrayTypeDimensionCount(type) == 1
        && isInternalPrimitiveType(ClassUtil.internalTypeFromArrayType(type))
        && concreteValue != null) {
      if (concreteValue instanceof Model) {
        throw new IllegalStateException(
            "Modeled arrays are not supported by ExecutingInvocationUnit");
      }
      return valueFactory.createArrayReferenceValue(
          type,
          null, // NB the class is null just because we are filtering for primitive arrays
          valueFactory.createIntegerValue(Array.getLength(concreteValue)),
          concreteValue);
    }

    boolean valueMayBeNull = concreteValue == null;
    AnalyzedObject resultObject =
        AnalyzedObjectFactory.create(concreteValue, type, referencedClass);
    if (valueId != null) {
      return valueFactory.createReferenceValueForId(
          referencedClass, valueMayBeExtension, valueMayBeNull, valueId, resultObject);
    }

    if (callerLocation != null) {
      return valueFactory.createReferenceValue(
          referencedClass, valueMayBeExtension, valueMayBeNull, callerLocation, resultObject);
    }

    return valueFactory.createReferenceValue(
        referencedClass, valueMayBeExtension, valueMayBeNull, resultObject);
  }

  /**
   * Create a {@link Value} possibly with a known reference identifier.
   *
   * @param type the static type of the created value (runtime type might implement/extend it).
   * @param referencedClass the {@link Clazz} of the value (if it's a reference value).
   * @param valueId the already known reference identifier of the created value. Null if the
   *     identifier was not previously known.
   * @return The non-particular {@link Value} corresponding to the given parameters.
   */
  private @NotNull Value createNonParticularValue(
      String type, Clazz referencedClass, boolean valueMayBeExtension, @Nullable Object valueId) {

    if (type.charAt(0) == VOID) {
      throw new IllegalStateException("A value should not be created for void type");
    }

    if (isInternalPrimitiveType(type)) {
      return valueFactory.createValue(type, null, false, false);
    }

    if (valueId != null) {
      return valueFactory.createReferenceValueForId(
          type, referencedClass, valueMayBeExtension, true, valueId);
    }

    return valueFactory.createValue(type, referencedClass, valueMayBeExtension, true);
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
   * Iterate the variables and replace all occurrences with the same reference id of the specified
   * value with its updated value.
   *
   * <p>This is expected to be used just for values that have a reference identifier available
   * (i.e., they are specific).
   *
   * @param newValue the value which should be put in.
   * @param variables the variables to look through.
   */
  private void replaceReferenceInVariables(Value newValue, Variables variables) {
    if (!newValue.isSpecific()) {
      throw new IllegalStateException("Can't identify a non specific value");
    }
    for (int i = 0; i < variables.size(); i++) {
      Value oldValue = variables.getValue(i);
      if (oldValue == null) {
        continue;
      }
      if (oldValue.isCategory2()) {
        i++;
      }
      if (!oldValue.isSpecific() || !(oldValue instanceof ReferenceValue)) {
        continue;
      }
      if (Objects.equals(
          PartialEvaluatorUtils.getIdFromSpecificReferenceValue(oldValue.referenceValue()),
          PartialEvaluatorUtils.getIdFromSpecificReferenceValue(newValue.referenceValue()))) {
        variables.store(i, newValue);
      }
    }
  }

  /**
   * Iterate the stack and replace all occurrences with the same reference id of the specified value
   * with its updated value.
   *
   * <p>This is expected to be used just for values that have a reference identifier available
   * (i.e., they are specific).
   *
   * @param newValue the value which should be put in.
   * @param stack the variables to look through.
   */
  private void replaceReferenceOnStack(Value newValue, Stack stack) {
    if (!newValue.isSpecific()) {
      throw new IllegalStateException("Can't identify a non specific value");
    }
    for (int i = 0; i < stack.size(); i++) {
      Value oldValue = stack.getTop(i);
      if (oldValue == null || !oldValue.isSpecific() || !(oldValue instanceof ReferenceValue)) {
        continue;
      }
      if (Objects.equals(
          PartialEvaluatorUtils.getIdFromSpecificReferenceValue(oldValue.referenceValue()),
          PartialEvaluatorUtils.getIdFromSpecificReferenceValue(newValue.referenceValue()))) {
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
    public void visitAnyMember(Clazz clazz, Member member) {
      // We are not interested in generic members.
    }

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField) {
      this.currentField = programField;
      programField.attributesAccept(programClass, this);
    }

    // Implementations for AttributeVisitor

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {
      // We are not interested in generic attributes.
    }

    @Override
    public void visitConstantValueAttribute(
        Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute) {
      clazz.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex, this);
    }

    // Implementations for ConstantVisitor

    @Override
    public void visitAnyConstant(Clazz clazz, Constant constant) {
      // We are not interested in generic constants.
    }

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
              currentField.referencedClass,
              isExtendable(currentField.referencedClass),
              false,
              AnalyzedObjectFactory.createPrecise(stringConstant.getString(clazz)));
    }
  }
}
