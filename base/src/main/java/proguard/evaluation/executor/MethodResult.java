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

import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * This class is a generic wrapper for a result. It is similar to {@link java.util.Optional} with a
 * key difference: An instance of this class is allowed to have a value of null and be non-empty.
 * This type can also convey the notion of failure with null still being a valid value.
 *
 * @param <T> The type of the result that is wrapped.
 */
public class MethodResult<T> {
  private static final MethodResult<?> EMPTY = new MethodResult<>();

  @Nullable private final T value;
  boolean isPresent;

  private MethodResult() {
    value = null;
    this.isPresent = false;
  }

  private MethodResult(@Nullable T value) {
    this.value = value;
    isPresent = true;
  }

  /** Returns an empty {@link MethodResult} meaning there is no value present. */
  public static <T> MethodResult<T> empty() {
    @SuppressWarnings("unchecked")
    MethodResult<T> t = (MethodResult<T>) EMPTY;
    return t;
  }

  /**
   * Returns a {@link MethodResult} that wraps the given value. This method will never return empty.
   */
  public static <T> MethodResult<T> of(@Nullable T value) {
    return new MethodResult<>(value);
  }

  /** Returns if a value is present */
  public boolean isPresent() {
    return isPresent;
  }

  /** Returns if no value is present */
  public boolean isEmpty() {
    return !isPresent;
  }

  /** Applies the mapper function to the value if it is present. */
  public <U> MethodResult<U> map(Function<? super T, ? extends U> mapper) {
    Objects.requireNonNull(mapper);
    if (!isPresent) {
      return empty();
    } else {
      return MethodResult.of(mapper.apply(value));
    }
  }

  /** If a value is present return it, else <code>other</code> is returned. */
  public T orElse(T other) {
    return isPresent ? value : other;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof MethodResult<?>)) {
      return false;
    }

    return Objects.equals(value, ((MethodResult<?>) obj).value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return value != null ? ("MethodResult[" + value + "]") : "MethodResult.empty";
  }
}
