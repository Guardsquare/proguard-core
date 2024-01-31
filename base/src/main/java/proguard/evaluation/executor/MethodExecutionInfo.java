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
import proguard.analysis.datastructure.CodeLocation;
import proguard.analysis.datastructure.callgraph.ConcreteCall;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.MethodSignature;
import proguard.classfile.constant.AnyMethodrefConstant;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ReturnClassExtractor;
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
  private final String returnType;
  private final Clazz resultClass;
  private final String resultType;

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param clazz The referenced class.
   * @param method The referenced method.
   * @param caller The code location of the call site. May be null.
   */
  public MethodExecutionInfo(Clazz clazz, Method method, CodeLocation caller) {
    signature = new MethodSignature(clazz, method);
    this.caller = caller;

    returnType = ClassUtil.internalMethodReturnType(signature.descriptor.toString());
    isConstructor = signature.method.equals(METHOD_NAME_INIT);
    isStatic = (method.getAccessFlags() & STATIC) != 0;

    if (isConstructor) {
      // For a Constructor, we always return a type, even if the return type of the method would be
      // void.
      resultClass = clazz;
    } else {
      // Get the class of the return type, if available, null otherwise.
      ReturnClassExtractor returnClassExtractor = new ReturnClassExtractor();
      method.accept(clazz, returnClassExtractor);
      resultClass = returnClassExtractor.returnClass;
    }
    resultType =
        resultClass != null && isConstructor
            ? ClassUtil.internalTypeFromClassName(resultClass.getName())
            : returnType;
  }

  /**
   * Constructs a MethodExecutionInfo.
   *
   * @param anyMethodrefConstant A method reference constant. Requires referenced class to be
   *     initialized (using {@link proguard.classfile.util.ClassReferenceInitializer}.
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

  /** Get the return type of the method. */
  public String getReturnType() {
    return returnType;
  }

  /**
   * Get the result class of the method. If the method is a constructor, this will be the class of
   * the instance.
   */
  public Clazz getResultClass() {
    return resultClass;
  }

  /**
   * Get the result type of the method. If the method is a constructor, this will be the type of the
   * instance.
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
  public Optional<Object> getCallingInstance(Executor executor, Value[] parameters) {
    return getInstanceValue(parameters)
        .flatMap(instanceValue -> getCallingInstance(executor, instanceValue));
  }

  /** Get the calling instance using the copy behavior defined by the executor. */
  public Optional<Object> getCallingInstance(Executor executor, ReferenceValue instanceValue) {
    return (instanceValue != null && instanceValue.isParticular() && !isConstructor)
        ? Optional.of(executor.getInstanceCopyIfMutable(instanceValue, signature.getClassName()))
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
    return getInstanceValue(instance)
        .map(
            instanceValue ->
                getInstanceType(instanceValue)
                    .map(instanceType -> instanceType.equals(returnType))
                    .orElse(false))
        .orElse(false);
  }
}
