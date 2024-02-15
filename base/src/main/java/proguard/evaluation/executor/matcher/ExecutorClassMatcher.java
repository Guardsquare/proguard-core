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

import proguard.classfile.MethodSignature;
import proguard.evaluation.executor.Executor;
import proguard.util.StringMatcher;

/**
 * This {@link ExecutorMatcher} matches solely based on the class name using a given {@link
 * StringMatcher}. Use if the {@link Executor} supports a range of classes fully.
 */
public class ExecutorClassMatcher implements ExecutorMatcher {
  private final StringMatcher classNameMatcher;

  /**
   * Creates a new {@link ExecutorClassMatcher} that all matches all classes that match with the
   * given {@link StringMatcher}
   */
  public ExecutorClassMatcher(StringMatcher classNameMatcher) {
    this.classNameMatcher = classNameMatcher;
  }

  @Override
  public boolean matches(MethodSignature signature) {
    return classNameMatcher.matches(signature.getClassName());
  }
}
