/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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

import java.util.Objects;
import proguard.classfile.Clazz;
import proguard.evaluation.ReferenceTracingValueFactory;

/**
 * This {@link ReferenceValue} represents a reference value that is tagged with a trace value.
 *
 * @author Eric Lafortune
 */
public class TracedReferenceValue extends ReferenceValue {
  private final ReferenceValue referenceValue;
  private final Value traceValue;
  private final int initialized;

  public TracedReferenceValue(ReferenceValue referenceValue, Value traceValue) {
    this(referenceValue, traceValue, ALWAYS);
  }

  /** Creates a new reference value with the given ID. */
  public TracedReferenceValue(ReferenceValue referenceValue, Value traceValue, int initialized) {
    this.referenceValue = referenceValue;
    this.traceValue = traceValue;
    this.initialized = initialized;
  }

  /** Returns the reference value. */
  public ReferenceValue getReferenceValue() {
    return referenceValue;
  }

  /** Returns the trace value. */
  public Value getTraceValue() {
    return traceValue;
  }

  /**
   * Returns whether the reference is initialized.
   *
   * @return <code>NEVER</code>, <code>MAYBE</code>, or <code>ALWAYS</code>.
   */
  public int isInitialized() {
    return initialized;
  }

  // Implementations for ReferenceValue.

  @Override
  public String getType() {
    return referenceValue.getType();
  }

  @Override
  public Clazz getReferencedClass() {
    return referenceValue.getReferencedClass();
  }

  @Override
  public boolean mayBeExtension() {
    return referenceValue.mayBeExtension();
  }

  @Override
  public int isNull() {
    return referenceValue.isNull();
  }

  @Override
  public int instanceOf(String otherType, Clazz otherReferencedClass) {
    return referenceValue.instanceOf(otherType, otherReferencedClass);
  }

  @Override
  public ReferenceValue cast(
      String type, Clazz referencedClass, ValueFactory valueFactory, boolean alwaysCast) {
    // We're letting the value factory do the cast (either preserving the
    // trace value or setting a new one).
    return ((ReferenceTracingValueFactory) valueFactory)
        .cast(this, type, referencedClass, alwaysCast);
  }

  @Override
  public IntegerValue arrayLength(ValueFactory valueFactory) {
    return referenceValue.arrayLength(valueFactory);
  }

  @Override
  public IntegerValue integerArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    return referenceValue.integerArrayLoad(indexValue, valueFactory);
  }

  @Override
  public LongValue longArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    return referenceValue.longArrayLoad(indexValue, valueFactory);
  }

  @Override
  public FloatValue floatArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    return referenceValue.floatArrayLoad(indexValue, valueFactory);
  }

  @Override
  public DoubleValue doubleArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    return referenceValue.doubleArrayLoad(indexValue, valueFactory);
  }

  @Override
  public ReferenceValue referenceArrayLoad(IntegerValue indexValue, ValueFactory valueFactory) {
    ReferenceValue value = referenceValue.referenceArrayLoad(indexValue, valueFactory);

    // We're keeping the existing trace value, if any, or attaching a new
    // one otherwise.
    return value instanceof TracedReferenceValue
        ? value
        : ((ReferenceTracingValueFactory) valueFactory).trace(value);
  }

  @Override
  public void arrayStore(IntegerValue indexValue, Value value) {
    referenceValue.arrayStore(indexValue, value);
  }

  // Implementations of binary methods of ReferenceValue.

  @Override
  public ReferenceValue generalize(ReferenceValue other) {
    return other.generalize(this);
  }

  @Override
  public int equal(ReferenceValue other) {
    return other.equal(this);
  }

  // Implementations of binary ReferenceValue methods with
  // UnknownReferenceValue arguments.

  @Override
  public ReferenceValue generalize(UnknownReferenceValue other) {
    return new TracedReferenceValue(referenceValue.generalize(other), traceValue);
  }

  @Override
  public int equal(UnknownReferenceValue other) {
    return referenceValue.equal(other);
  }

  // Implementations of binary ReferenceValue methods with
  // TypedReferenceValue arguments.

  @Override
  public ReferenceValue generalize(TypedReferenceValue other) {
    return new TracedReferenceValue(referenceValue.generalize(other), traceValue);
  }

  @Override
  public int equal(TypedReferenceValue other) {
    return referenceValue.equal(other);
  }

  // Implementations of binary ReferenceValue methods with
  // IdentifiedReferenceValue arguments.

  @Override
  public ReferenceValue generalize(IdentifiedReferenceValue other) {
    return new TracedReferenceValue(referenceValue.generalize(other), traceValue);
  }

  @Override
  public int equal(IdentifiedReferenceValue other) {
    return referenceValue.equal(other);
  }

  // Implementations of binary ReferenceValue methods with
  // ArrayReferenceValue arguments.

  @Override
  public ReferenceValue generalize(ArrayReferenceValue other) {
    return new TracedReferenceValue(referenceValue.generalize(other), traceValue);
  }

  @Override
  public int equal(ArrayReferenceValue other) {
    return referenceValue.equal(other);
  }

  // Implementations of binary ReferenceValue methods with
  // IdentifiedArrayReferenceValue arguments.

  @Override
  public ReferenceValue generalize(IdentifiedArrayReferenceValue other) {
    return new TracedReferenceValue(referenceValue.generalize(other), traceValue);
  }

  @Override
  public int equal(IdentifiedArrayReferenceValue other) {
    return referenceValue.equal(other);
  }

  // Implementations of binary ReferenceValue methods with
  // DetailedArrayReferenceValue arguments.

  @Override
  public ReferenceValue generalize(DetailedArrayReferenceValue other) {
    return new TracedReferenceValue(referenceValue.generalize(other), traceValue);
  }

  @Override
  public int equal(DetailedArrayReferenceValue other) {
    return referenceValue.equal(other);
  }

  // Implementations of binary ReferenceValue methods with
  // TracedReferenceValue arguments.

  @Override
  public ReferenceValue generalize(TracedReferenceValue other) {
    if (this.equals(other)) {
      return this;
    }

    return new TracedReferenceValue(
        this.referenceValue.generalize(other.referenceValue),
        this.traceValue.generalize(other.traceValue),
        this.initialized == other.initialized ? this.initialized : Value.MAYBE);
  }

  @Override
  public int equal(TracedReferenceValue other) {
    return this.referenceValue.equal(other.referenceValue);
  }

  // Implementations for Value.

  @Override
  public boolean isSpecific() {
    return referenceValue.isSpecific();
  }

  @Override
  public boolean isParticular() {
    return referenceValue.isParticular();
  }

  @Override
  public String internalType() {
    return referenceValue.internalType();
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

    TracedReferenceValue other = (TracedReferenceValue) object;
    return this.referenceValue.equals(other.referenceValue)
        && this.traceValue.equals(other.traceValue)
        && this.initialized == other.initialized;
  }

  @Override
  public int hashCode() {
    return Objects.hash(referenceValue, traceValue, initialized);
  }

  @Override
  public String toString() {
    return traceValue.toString() + referenceValue.toString() + " (init=" + initialized + ")";
  }
}
