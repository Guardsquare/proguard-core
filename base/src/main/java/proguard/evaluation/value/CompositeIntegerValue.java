/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
package proguard.evaluation.value;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** This {@link IntegerValue} represents the result of a binary operation on two integer values. */
public final class CompositeIntegerValue extends SpecificIntegerValue {
  public static final byte ADD = '+';
  public static final byte SUBTRACT = '-';
  public static final byte MULTIPLY = '*';
  public static final byte DIVIDE = '/';
  public static final byte REMAINDER = '%';
  public static final byte SHIFT_LEFT = '<';
  public static final byte SHIFT_RIGHT = '>';
  public static final byte UNSIGNED_SHIFT_RIGHT = '}';
  public static final byte AND = '&';
  public static final byte OR = '|';
  public static final byte XOR = '^';

  private final IntegerValue integerValue1;
  private final byte operation;
  private final IntegerValue integerValue2;

  private final int hash;

  public CompositeIntegerValue(
      IntegerValue integerValue1, byte operation, IntegerValue integerValue2) {
    this.integerValue1 = integerValue1;
    this.operation = operation;
    this.integerValue2 = integerValue2;

    this.hash = Objects.hash(super.hashCode(), integerValue1, operation, integerValue2);
  }

  // Implementations for Object.

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!super.equals(object)) {
      return false;
    }

    CompositeIntegerValue other = (CompositeIntegerValue) object;
    if (this.hash != other.hash || this.operation != other.operation) {
      return false;
    }

    Map<Object, Set<Object>> visited = new IdentityHashMap<>();

    return equalsMemoised(other, visited);
  }

  /**
   * Recursive comparison of nested IntegerValues. We use a map to keep track of which comparisons
   * have already been made in the past, to avoid duplicate work.
   */
  private boolean equalsMemoised(CompositeIntegerValue other, Map<Object, Set<Object>> visited) {
    IntegerValue a1 = this.integerValue1, a2 = this.integerValue2;
    IntegerValue b1 = other.integerValue1, b2 = other.integerValue2;

    if (a1 == a2 && b1 == b2) {
      return childEquals(a1, b1, visited);
    }
    return childEquals(a1, b1, visited) && childEquals(a2, b2, visited);
  }

  private static boolean childEquals(
      IntegerValue a, IntegerValue b, Map<Object, Set<Object>> visited) {

    if (a == b) {
      return true;
    }

    if (a instanceof CompositeIntegerValue && b instanceof CompositeIntegerValue) {
      CompositeIntegerValue ca = (CompositeIntegerValue) a;
      CompositeIntegerValue cb = (CompositeIntegerValue) b;

      // Same checks equals() would do, without going through it.
      // We don't need to verify if .getClass() are equal since CompositeIntegerValue is final.
      if (ca.hash != cb.hash || ca.operation != cb.operation) {
        return false;
      }

      // Already compared in this equals() call: any mismatch would
      // have aborted the whole comparison, so it's safe to treat as equal.
      // Note: that since the map is not bidirectional, its possible that ca.equals(cb) is not found
      // while cb.equals(ca) already exists. Solving this properly, would require double the amount
      // of memory, which is overkill for a situation that won't happen that frequently.
      Set<Object> seen =
          visited.computeIfAbsent(ca, k -> Collections.newSetFromMap(new IdentityHashMap<>()));
      if (!seen.add(cb)) {
        return true;
      }
      return ca.equalsMemoised(cb, visited);
    }

    return a.equals(b);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    String type1 = (integerValue1 != null) ? integerValue1.getClass().getSimpleName() : "null";
    String type2 = (integerValue2 != null) ? integerValue2.getClass().getSimpleName() : "null";

    return "(" + type1 + ((char) operation) + type2 + ")";
  }
}
