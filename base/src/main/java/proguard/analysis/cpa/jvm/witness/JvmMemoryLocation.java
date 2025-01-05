/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

package proguard.analysis.cpa.jvm.witness;

import proguard.analysis.cpa.interfaces.AbstractState;
import proguard.analysis.cpa.jvm.state.JvmAbstractState;

/**
 * A {@link JvmMemoryLocation} points at a specific location in a certain state of the Jvm. For
 * example a {@link JvmStackLocation} with index 0 indicates the top of the stack.
 */
public abstract class JvmMemoryLocation {

  /**
   * Given a JVM state, extract its content in the position represented by this {@link
   * JvmMemoryLocation}.
   *
   * @param jvmState The state from which the value is extracted.
   * @param defaultValue The value returned if it's not possible to extract the value.
   * @return The value from the JVM abstract state for the memory location represented by this
   *     object. Or default value if not possible.
   * @param <T> The type of the states contained in the JVM state. e.g., for taint analysis this
   *     would be a {@link proguard.analysis.cpa.defaults.SetAbstractState} containing the taints
   *     and for value analysis a {@link proguard.analysis.cpa.jvm.domain.value.ValueAbstractState}.
   */
  public abstract <T extends AbstractState<T>> T extractValueOrDefault(
      JvmAbstractState<T> jvmState, T defaultValue);

  // implementations for Object

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public abstract String toString();
}
