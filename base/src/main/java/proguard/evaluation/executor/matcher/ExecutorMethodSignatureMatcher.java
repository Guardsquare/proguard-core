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
import proguard.util.FixedStringMatcher;
import proguard.util.OrMatcher;
import proguard.util.StringMatcher;

/**
 * This {@link ExecutorMatcher} matches using a mapping from classes to methods to a {@link
 * StringMatcher} for the descriptor. Use if only certain method overloads are supported.
 */
public class ExecutorMethodSignatureMatcher implements ExecutorMatcher {
  private final Map<String, Map<String, StringMatcher>> methodMatchers;

  public ExecutorMethodSignatureMatcher(Map<String, Map<String, StringMatcher>> methodMatchers) {
    this.methodMatchers = methodMatchers;
  }

  /** Builds an {@link ExecutorMethodSignatureMatcher}. */
  public static class Builder {
    private final Map<String, Map<String, StringMatcher>> methodMatchers;

    public Builder() {
      methodMatchers = new HashMap<>();
    }

    /**
     * Add a match to the mapping. If a matcher for class names already has an associated matcher
     * for method names the old and the given matcher are combined using an {@link OrMatcher}.
     *
     * @param className The name of the class.
     * @param methodName The name of the method.
     * @param descriptorMatcher The matcher for the descriptor.
     */
    public Builder addMethodMatch(
        String className, String methodName, StringMatcher descriptorMatcher) {
      methodMatchers
          .computeIfAbsent(className, name -> new HashMap<>())
          .merge(methodName, descriptorMatcher, OrMatcher::new);
      return this;
    }

    /**
     * Add a match for the specified method.
     *
     * @param signature The signature of the method to match.
     */
    public Builder addMethodMatch(MethodSignature signature) {
      return addMethodMatch(
          signature.getClassName(),
          signature.method,
          new FixedStringMatcher(signature.descriptor.toString()));
    }

    /**
     * Build the {@link ExecutorMethodMatcher} defined by this builder.
     *
     * @return The built matcher.
     */
    public ExecutorMethodSignatureMatcher build() {
      return new ExecutorMethodSignatureMatcher(methodMatchers);
    }
  }

  @Override
  public boolean matches(MethodSignature signature) {
    Map<String, StringMatcher> descriptorMatchers = methodMatchers.get(signature.getClassName());
    if (descriptorMatchers == null) return false;
    StringMatcher descriptorMatcher = descriptorMatchers.get(signature.method);
    return descriptorMatcher != null && descriptorMatcher.matches(signature.descriptor.toString());
  }
}
