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
import proguard.classfile.MethodSignature;
import proguard.evaluation.ExecutingInvocationUnit;
import proguard.evaluation.executor.instancehandler.ExecutorInstanceHandler;
import proguard.evaluation.executor.matcher.ExecutorMatcher;
import proguard.evaluation.value.ReferenceValue;
import proguard.evaluation.value.Value;

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
   * @return The result of the method call wrapped in an optional. <code>Optional.empty()</code> if
   *     a result could not be calculated.
   */
  public abstract Optional<Object> getMethodResult(
      MethodExecutionInfo methodData,
      ReferenceValue instance,
      Object callingInstance,
      Value[] parameters);

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
   * @param className The name of the class of the instance.
   * @return The new calling instance.
   */
  public abstract Optional<Object> getInstanceCopyIfMutable(
      ReferenceValue instanceValue, String className);
}
