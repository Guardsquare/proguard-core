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

import static proguard.classfile.AccessConstants.STATIC;
import static proguard.classfile.ClassConstants.METHOD_NAME_INIT;
import static proguard.classfile.TypeConstants.VOID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import proguard.analysis.datastructure.CodeLocation;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.Signature;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ReferencedClassesExtractor;
import proguard.evaluation.ValueCalculator;
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;

/**
 * This class stores data relevant to modeling the execution of a method and offers methods to
 * extract additional information.
 */
public class MethodExecutionInfo {
  private final MethodSignature signature;
  private final CodeLocation caller;
  private final boolean isConstructor;
  private final boolean isStatic;
  private final @NotNull Clazz targetClass;
  private final Clazz returnClass;
  private final List<Clazz> parametersClasses;
  private final @Nullable ReferenceValue instance;
  /** NB: the calling instance is not included in the parameters. */
  private final List<Value> parameters;
  /** Lazily initialized field, available inside the executors. */
  private @Nullable MethodSignature resolvedTargetSignature = null;

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param clazz The referenced class.
   * @param method The referenced method.
   * @param caller The code location of the call site. May be null.
   * @param parameters The parameters of the call, calling instance included.
   */
  public MethodExecutionInfo(
      @NotNull Clazz clazz,
      @NotNull Method method,
      @Nullable CodeLocation caller,
      @NotNull Value... parameters) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(method);
    Objects.requireNonNull(parameters);
    targetClass = clazz;
    signature = (MethodSignature) Signature.of(clazz, method);
    this.caller = caller;

    isConstructor = signature.method.equals(METHOD_NAME_INIT);
    isStatic = (method.getAccessFlags() & STATIC) != 0;

    ReferencedClassesExtractor referencedClassesExtractor = new ReferencedClassesExtractor();
    method.accept(clazz, referencedClassesExtractor);

    returnClass = referencedClassesExtractor.getReturnClass();
    parametersClasses =
        Collections.unmodifiableList(
            Arrays.asList(referencedClassesExtractor.getParameterClasses()));

    ReferenceValue instanceParameter = null;
    if (!isStatic) {
      Value instanceValue = parameters[0];
      if (!(instanceValue instanceof ReferenceValue)) {
        throw new IllegalArgumentException(
            "Methods should not be executed with ExecutingInvocationUnit if the instance is unknown");
      }
      instanceParameter = parameters[0].referenceValue();
    }
    int parametersOffset = isStatic ? 0 : 1;
    List<Value> nonInstanceParameters =
        Arrays.stream(parameters).skip(parametersOffset).collect(Collectors.toList());

    this.instance = instanceParameter;
    this.parameters = Collections.unmodifiableList(nonInstanceParameters);

    if (this.parametersClasses.size() != this.parameters.size()) {
      throw new IllegalStateException(
          "parametersClasses should be the same size as the non-instance parameters");
    }
  }

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param anyMethodrefConstant A method reference constant. Requires referenced class to be
   *     initialized (using {@link proguard.classfile.util.ClassReferenceInitializer}).
   * @param caller The code location of the call site. May be null.
   * @param parameters The parameters of the call, calling instance included.
   */
  public MethodExecutionInfo(
      AnyMethodrefConstant anyMethodrefConstant, CodeLocation caller, Value... parameters) {
    this(
        anyMethodrefConstant.referencedClass,
        anyMethodrefConstant.referencedMethod,
        caller,
        parameters);
  }

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param call the concrete call.
   * @param parameters The parameters of the call, calling instance included.
   */
  public MethodExecutionInfo(ConcreteCall call, Value... parameters) {
    this(call.getTargetClass(), call.getTargetMethod(), call.caller, parameters);
  }

  /** Get the method signature of the method */
  public MethodSignature getSignature() {
    return signature;
  }

  /**
   * Get the code location of the call site. Might not be provided depending on if the analysis
   * needs it or not
   */
  public @Nullable CodeLocation getCaller() {
    return caller;
  }

  /** Return whether the method is a constructor. */
  public boolean isConstructor() {
    return isConstructor;
  }

  /** Return whether the method is static. */
  public boolean isStatic() {
    return isStatic;
  }

  public boolean isInstanceMethod() {
    return !(isConstructor || isStatic);
  }

  /**
   * Get the return class of the method.
   *
   * <p>This is the return class as declared in the invoked method descriptor, method execution
   * might provide a more specific runtime type.
   *
   * @return The return referenced class. Can be null if the class pools have not been initialized;
   *     even if they have, the clazz not being null is not a guarantee.
   */
  public @Nullable Clazz getReturnClass() {
    return returnClass;
  }

  /**
   * Returns the referenced {@link Clazz} for each parameter.
   *
   * @return The referenced class for each parameter (instance excluded), where each parameter can
   *     be accessed given its position (starting from 0, category 2 values take only one slot). An
   *     element is null if the corresponding parameter is of a primitive type or an array of
   *     primitives. An element can be null if the class pools have not been initialized; even if
   *     they have, elements not being null is not a guarantee
   */
  public List<Clazz> getParametersClasses() {
    return parametersClasses;
  }

  /**
   * Returns the referenced {@link Clazz} of the target method. Corresponds to the invocation
   * instance class for instance methods and constructor calls.
   *
   * @return The target referenced class. Can't be null since this is a condition for the execution
   *     to be possible.
   */
  public @NotNull Clazz getTargetClass() {
    return targetClass;
  }

  /**
   * Get the static target type of the method. For constructors this corresponds to the type of the
   * constructed object.
   */
  public @NotNull String getTargetType() {
    return ClassUtil.internalMethodReturnType(
        ClassUtil.internalTypeFromClassName(targetClass.getName()));
  }

  /**
   * Get the static return type of the method.
   *
   * <p>This is the return type as declared in the invoked method constructor, method execution
   * might provide a more specific runtime type.
   */
  public String getReturnType() {
    return ClassUtil.internalMethodReturnType(signature.descriptor.toString());
  }

  /** Get the type of the instance, or empty for static methods. */
  public Optional<String> getInstanceType() {
    return instance != null ? Optional.of(instance.internalType()) : Optional.empty();
  }

  /** Return whether the return and instance types of the method match. */
  public boolean returnsSameTypeAsInstance() {
    if (!isInstanceMethod()) {
      return false;
    }
    Optional<String> instanceType = getInstanceType();
    if (!instanceType.isPresent()) {
      throw new IllegalStateException("Instance type should be present for instance methods");
    }
    return instanceType.get().equals(getReturnType());
  }

  /** Whether the method returns void. */
  public boolean returnsVoid() {
    return getReturnType().charAt(0) == VOID;
  }

  /** Returns the calling instance value of the method, throws if the method is static. */
  public @NotNull ReferenceValue getInstanceNonStatic() {
    if (isStatic) {
      throw new IllegalStateException("Do not try to retrieve the instance of a static method");
    }
    Objects.requireNonNull(instance);
    return instance;
  }

  /**
   * Returns the specific calling instance value of the method. Should only be called if the
   * instance is known to be present and specific, will throw otherwise.
   */
  public @NotNull IdentifiedReferenceValue getSpecificInstance() {
    if (isStatic) {
      throw new IllegalStateException("Do not try to retrieve the instance of a static method");
    }
    Objects.requireNonNull(instance);
    if (!instance.isSpecific()) {
      throw new IllegalStateException(
          "You should not use this method for instances that might be non specific");
    }
    Objects.requireNonNull(instance);
    return (IdentifiedReferenceValue) instance;
  }

  /** Returns the calling instance value of the method, or null if the method is static. */
  public @Nullable ReferenceValue getInstanceOrNullIfStatic() {
    return instance;
  }

  /** Returns the parameters of the method, calling instance not included. */
  public List<Value> getParameters() {
    return parameters;
  }

  /**
   * Gets the resolved target of the method call. For constructors and static methods this
   * corresponds to the static signature, while dynamic method resolution is performed by the
   * invocation unit for instance methods (i.e., the class in the signature is the calculated
   * runtime type of the instance).
   *
   * <p>This property is lazily initialized and is guaranteed to be available while {@link
   * Executor#getMethodResult(MethodExecutionInfo, ValueCalculator)} is running.
   *
   * @throws IllegalStateException if called before the initialization of the property.
   */
  public MethodSignature getResolvedTargetSignature() {
    if (resolvedTargetSignature == null) {
      throw new IllegalStateException("The dynamic target signature has not been initialized yet");
    }
    return resolvedTargetSignature;
  }

  /**
   * Sets the lazy property containing the resolved target of the method calls. For constructors and
   * static methods this corresponds to the static signature, while dynamic method resolution is
   * performed by the invocation unit for instance methods (i.e., the class in the signature is the
   * calculated runtime type of the instance).
   */
  public void setResolvedTargetSignature(@NotNull MethodSignature resolvedTargetSignature) {
    Objects.requireNonNull(resolvedTargetSignature);
    this.resolvedTargetSignature = resolvedTargetSignature;
  }
}
