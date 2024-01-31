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

/**
 * This interface provides a method to check whether a certain method call matches with a criterion
 * given by the implementation.
 */
public interface ExecutorMatcher {

  /**
   * Returns whether a method matches.
   *
   * @param signature The method signature.
   * @return whether the method matches.
   */
  boolean matches(MethodSignature signature);
}
