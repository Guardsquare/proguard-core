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

package proguard.evaluation.executor.matcher;

import java.util.HashMap;
import java.util.Map;
import proguard.classfile.MethodSignature;
import proguard.evaluation.executor.Executor;
import proguard.util.OrMatcher;
import proguard.util.StringMatcher;

/**
 * This {@link ExecutorMatcher} matches using a mapping from a class name to a {@link StringMatcher}
 * for matching method names. Use if the {@link Executor} handles a selection of methods from
 * specific classes.
 */
public class ExecutorMethodMatcher implements ExecutorMatcher {
  private final Map<String, StringMatcher> methodMatchers;

  /**
   * Creates an {@link ExecutorMethodMatcher} that supports methods given by a mapping from a
   * specific class name to supported method names given by a {@link StringMatcher}
   *
   * @param methodMatchers The mapping of classes to supported methods.
   */
  public ExecutorMethodMatcher(Map<String, StringMatcher> methodMatchers) {
    this.methodMatchers = methodMatchers;
  }

  /** Builds an {@link ExecutorMethodMatcher} */
  public static class Builder {
    private final Map<String, StringMatcher> methodMatchers = new HashMap<>();

    /**
     * Add a match to the mapping. If a {@link StringMatcher} is already mapped to a class name the
     * old and the given matcher are combined using an {@link OrMatcher}.
     *
     * @param className The name of the class.
     * @param methodMatcher The matcher for method names.
     */
    public Builder addMethodMatch(String className, StringMatcher methodMatcher) {
      methodMatchers.merge(className, methodMatcher, OrMatcher::new);
      return this;
    }

    /**
     * Build the {@link ExecutorMethodMatcher} defined by this builder.
     *
     * @return The built matcher.
     */
    public ExecutorMethodMatcher build() {
      return new ExecutorMethodMatcher(methodMatchers);
    }
  }

  @Override
  public boolean matches(MethodSignature methodSignature) {
    StringMatcher matcher = methodMatchers.get(methodSignature.getClassName());
    return matcher != null && matcher.matches(methodSignature.method);
  }
}
