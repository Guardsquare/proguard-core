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

import java.util.Optional;
import java.util.function.Function;
import proguard.classfile.MethodSignature;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.executor.instancehandler.ExecutorInstanceHandler;
import proguard.evaluation.executor.matcher.ExecutorMatcher;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This abstract class specifies a modular component which can be added to a {@link
 * ExecutingInvocationUnit} in order to extend its capabilities. An {@link Executor} specifies which
 * method calls it supports, what methods return their own instance and how a method result is
 * calculated.
 */
public abstract class Executor {
  protected ExecutorMatcher executorMatcher;
  protected ExecutorInstanceHandler instanceHandler;

  /**
   * Calculate the result of a given method. This is the return value for a non-constructor method
   * (<c>null</c> if it returns <c>void</c>) and the instantiated object for a constructor.
   *
   * @param methodData Information about the called method.
   * @param instance The {@link ReferenceValue} of the instance, <c>null</c> for static methods.
   * @param callingInstance The instance object, <c>null</c> for static methods.
   * @param parameters An array of the parameter values of the method call.
   * @param valueCalculator a function mapping the result of a method invocation (can be an Object
   *     with the result if the executor calculates a real value or a {@link
   *     proguard.evaluation.value.object.Model}) to the appropriate {@link Value} used by the
   *     analysis.
   * @return The result of the method call wrapped in an optional. <code>Optional.empty()</code> if
   *     a result could not be calculated.
   */
  public abstract Optional<Value> getMethodResult(
      MethodExecutionInfo methodData,
      ReferenceValue instance,
      AnalyzedObject callingInstance,
      Value[] parameters,
      Function<Object, Value> valueCalculator);

  /**
   * Returns whether a certain method invocation is supported.
   *
   * @param signature The method signature.
   * @return whether the method invocation is supported.
   */
  public boolean isSupportedMethodCall(MethodSignature signature) {
    return executorMatcher.matches(signature);
  }

  /**
   * Returns whether a certain method invocation is assumed to return its own instance. This
   * indicates whether replacing the instance reference in the stack and variables is necessary.
   *
   * @param signature The method signature.
   * @param returnsSameTypeAsInstance whether the method has matching return and instance types
   * @return whether the method returns its own instance.
   */
  public boolean returnsOwnInstance(MethodSignature signature, boolean returnsSameTypeAsInstance) {
    return returnsSameTypeAsInstance
        && instanceHandler.returnsOwnInstance(signature.getClassName(), signature.method);
  }

  /**
   * Get an object which will act as the calling instance. If we know that executing the method does
   * not modify the instance this can just be the same value. Otherwise, a copy should be returned
   * in order to not change the reference whose state may be of interest at the end of an analysis.
   *
   * @param instanceValue The {@link ReferenceValue} of the instance.
   * @return The new calling instance.
   */
  public abstract Optional<AnalyzedObject> getInstanceCopyIfMutable(ReferenceValue instanceValue);

  /**
   * A builder for the executor. It's important for each concrete executor to provide one in order
   * to be able to construct a fresh copy of the executor when needed
   *
   * <p>For example this happens in {@link proguard.evaluation.ExecutingInvocationUnit.Builder},
   * since each invocation unit should use different executors in order to avoid undesired side
   * effects.
   */
  public abstract static class Builder<T extends Executor> {

    /**
     * Build an executor. If the executor keeps internal state this method needs to guarantee that
     * no data is shared between the different implementations (unless needed by design, e.g., for
     * caching).
     */
    public abstract T build();
  }
}
