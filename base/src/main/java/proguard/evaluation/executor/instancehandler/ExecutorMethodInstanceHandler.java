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

package proguard.evaluation.executor.instancehandler;

import java.util.Map;
import proguard.util.StringMatcher;

/**
 * This {@link ExecutorInstanceHandler} decides whether a method always returns its calling instance
 * based on a mapping of class names to a {@link StringMatcher} for method names.
 */
public class ExecutorMethodInstanceHandler implements ExecutorInstanceHandler {
  private final Map<String, StringMatcher> alwaysReturnsOwnInstance;

  /**
   * Creates an {@link ExecutorMethodInstanceHandler} using the given mapping.
   *
   * @param alwaysReturnsOwnInstance A mapping from class names to a matcher for methods that return
   *     a new instance
   */
  public ExecutorMethodInstanceHandler(Map<String, StringMatcher> alwaysReturnsOwnInstance) {
    this.alwaysReturnsOwnInstance = alwaysReturnsOwnInstance;
  }

  @Override
  public boolean returnsOwnInstance(String internalClassName, String methodName) {
    StringMatcher matcher = alwaysReturnsOwnInstance.get(internalClassName);
    return matcher != null && matcher.matches(methodName);
  }
}
