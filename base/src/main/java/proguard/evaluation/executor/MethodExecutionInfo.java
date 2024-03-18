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

import java.util.Optional;
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
import proguard.evaluation.value.IdentifiedReferenceValue;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This class stores data relevant to modeling the execution of a method and offers methods to
 * extract additional information.
 */
public class MethodExecutionInfo {
  private final MethodSignature signature;
  private final CodeLocation caller;
  private final boolean isConstructor;
  private final boolean isStatic;
  private final Clazz targetClass;
  private final Clazz resultClass;
  private final Clazz[] parametersClasses;
  private final String resultType;

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param clazz The referenced class.
   * @param method The referenced method.
   * @param caller The code location of the call site. May be null.
   */
  public MethodExecutionInfo(Clazz clazz, Method method, CodeLocation caller) {
    targetClass = clazz;
    signature = (MethodSignature) Signature.of(clazz, method);
    this.caller = caller;

    isConstructor = signature.method.equals(METHOD_NAME_INIT);
    isStatic = (method.getAccessFlags() & STATIC) != 0;

    ReferencedClassesExtractor referencedClassesExtractor = new ReferencedClassesExtractor();
    method.accept(clazz, referencedClassesExtractor);

    if (isConstructor) {
      // For a Constructor, we always return a type, even if the return type of the method would be
      // void.
      resultClass = clazz;
    } else {
      // Get the class of the return type, if available, null otherwise.
      resultClass = referencedClassesExtractor.getReturnClass();
    }

    parametersClasses = referencedClassesExtractor.getParameterClasses();

    resultType =
        resultClass != null && isConstructor
            ? ClassUtil.internalTypeFromClassName(resultClass.getName())
            : ClassUtil.internalMethodReturnType(signature.descriptor.toString());
  }

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param anyMethodrefConstant A method reference constant. Requires referenced class to be
   *     initialized (using {@link proguard.classfile.util.ClassReferenceInitializer}).
   * @param caller The code location of the call site. May be null.
   */
  public MethodExecutionInfo(AnyMethodrefConstant anyMethodrefConstant, CodeLocation caller) {
    this(anyMethodrefConstant.referencedClass, anyMethodrefConstant.referencedMethod, caller);
  }

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param call the concrete call.
   */
  public MethodExecutionInfo(ConcreteCall call) {
    this(call.getTargetClass(), call.getTargetMethod(), call.caller);
  }

  /** Get the method signature of the method */
  public MethodSignature getSignature() {
    return signature;
  }

  /** Get the code location of the call site */
  public Optional<CodeLocation> getCaller() {
    return Optional.ofNullable(caller);
  }

  /** Return whether the method is a constructor. */
  public boolean isConstructor() {
    return isConstructor;
  }

  /** Return whether the method is static. */
  public boolean isStatic() {
    return isStatic;
  }

  /**
   * Get the result class of the method. If the method is a constructor, this will be the class of
   * the instance.
   *
   * <p>This is the result class as declared in the invoked method constructor, method execution
   * might provide a more specific runtime type.
   *
   * @return The return referenced class. Can be null if the class pools have not been initialized;
   *     even if they have, the clazz not being null is not a guarantee.
   */
  public @Nullable Clazz getResultClass() {
    return resultClass;
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
  public Clazz[] getParametersClasses() {
    return parametersClasses;
  }

  /**
   * Returns the referenced {@link Clazz} of the target method. Corresponds to the invocation
   * instance class for instance methods and constructor calls.
   *
   * @return The target referenced class. Can be null if the class pools have not been initialized;
   *     even if they have, the clazz not being null is not a guarantee.
   */
  public @Nullable Clazz getTargetClass() {
    return targetClass;
  }

  /**
   * Get the result type of the method. If the method is a constructor, this will be the type of the
   * instance.
   *
   * <p>This is the result type as declared in the invoked method constructor, method execution
   * might provide a more specific runtime type.
   */
  public String getResultType() {
    return resultType;
  }

  /** Get the {@link ReferenceValue} of the instance. */
  public Optional<ReferenceValue> getInstanceValue(Value instance) {
    return instance instanceof ReferenceValue
        ? Optional.of(instance.referenceValue())
        : Optional.empty();
  }

  /** Get the reference value of the instance. */
  public Optional<ReferenceValue> getInstanceValue(Value[] parameters) {
    return parameters != null && !isStatic ? getInstanceValue(parameters[0]) : Optional.empty();
  }

  /** Get the object ID for the instance value. */
  public Optional<Object> getObjectId(ReferenceValue instanceValue) {
    return instanceValue instanceof IdentifiedReferenceValue
        ? Optional.of(((IdentifiedReferenceValue) instanceValue).id)
        : Optional.empty();
  }

  /** Get the calling instance using the copy behavior defined by the executor. */
  public Optional<AnalyzedObject> getCallingInstance(Executor executor, Value[] parameters) {
    return getInstanceValue(parameters)
        .flatMap(instanceValue -> getCallingInstance(executor, instanceValue));
  }

  /** Get the calling instance using the copy behavior defined by the executor. */
  public Optional<AnalyzedObject> getCallingInstance(
      Executor executor, ReferenceValue instanceValue) {
    return (instanceValue != null && instanceValue.isParticular() && !isConstructor)
        ? executor.getInstanceCopyIfMutable(instanceValue)
        : Optional.empty();
  }

  /** Get the type of the instance. */
  public Optional<String> getInstanceType(ReferenceValue instanceValue) {
    return instanceValue != null ? Optional.of(instanceValue.internalType()) : Optional.empty();
  }

  /** Return whether the return and instance types of the method match. */
  public boolean returnsSameTypeAsInstance(Value[] parameters) {
    return getInstanceValue(parameters).map(this::returnsSameTypeAsInstance).orElse(false);
  }

  /** Return whether the return and instance types of the method match. */
  public boolean returnsSameTypeAsInstance(Value instance) {
    return getInstanceValue(instance).map(this::returnsSameTypeAsInstance).orElse(false);
  }

  /** Return whether the return and instance types of the method match. */
  public boolean returnsSameTypeAsInstance(ReferenceValue instance) {
    // This condition is important as long as resultType for constructor is the instance type
    // instead of "V".
    if (isConstructor) {
      return false;
    }
    return getInstanceValue(instance)
        .map(
            instanceValue ->
                getInstanceType(instanceValue)
                    .map(instanceType -> instanceType.equals(resultType))
                    .orElse(false))
        .orElse(false);
  }
}
