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

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.evaluation.value.object.AnalyzedObject;

/**
 * This {@link ParticularReferenceValue} represents a particular reference value, i.e. a reference
 * with an associated value. E.g., a String with the value "HelloWorld".
 */
public class ParticularReferenceValue extends IdentifiedReferenceValue {
  public static boolean ARRAY_EXCEPTIONS =
      System.getProperty("proguard.particularvalue.arrayexceptions") != null;

  // The actual value of the object.
  private final AnalyzedObject objectValue;

  /**
   * Create a new Instance with the given type, the class it is referenced in, and its actual value.
   */
  public ParticularReferenceValue(
      Clazz referencedClass,
      ValueFactory valueFactory,
      Object referenceID,
      @NotNull AnalyzedObject value) {
    // We store the unique ID to keep track of the same value (independent of casting and
    // generalizations) on stack and vars.
    // This ID is needed, since the generalization and casting might create new instances, and we
    // need to see that these were in fact the ones we need to replace.
    super(value.getType(), referencedClass, false, true, valueFactory, referenceID);

    Objects.requireNonNull(value);
    Objects.requireNonNull(
        value.getType(),
        "ParticularReferenceValue should not be created with a 'NullObject', a 'TypedReferenceValue' with null type is expected in that case");

    if (ARRAY_EXCEPTIONS
        && proguard.classfile.util.ClassUtil.isInternalArrayType(value.getType())) {
      throw new IllegalStateException(
          "ParticularReferenceValue should not be used for arrays use DetailedArrayReferenceValue instead");
    }

    this.objectValue = value;
  }

  // Implementations for ReferenceValue.

  /** Deprecated, use {@link ParticularReferenceValue#getValue()}. */
  @Override
  @Deprecated
  public Object value() {
    return objectValue.isModeled() ? objectValue.getModeledValue() : objectValue.getPreciseValue();
  }

  @Override
  public @NotNull AnalyzedObject getValue() {
    return objectValue;
  }

  // Implementations for TypedReferenceValue.

  @Override
  public boolean isParticular() {
    return true;
  }

  @Override
  public int isNull() {
    return objectValue.isNull() ? ALWAYS : NEVER;
  }

  @Override
  public int instanceOf(String otherType, Clazz otherReferencedClass) {
    // If the value extends the type, we're sure (unless it may be null).
    // Otherwise, if the value type is final, it can never be an instance.
    // Also, if the types are not interfaces and not in the same hierarchy,
    // the value can never be an instance.
    if (referencedClass == null || otherReferencedClass == null) {
      return MAYBE;
    } else {
      if (referencedClass.extendsOrImplements(otherReferencedClass)) {
        return ALWAYS;
      } else {
        if ((referencedClass.getAccessFlags() & AccessConstants.FINAL) != 0) {
          return NEVER;
        }
        if ((referencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0
            && (otherReferencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0
            && !otherReferencedClass.extendsOrImplements(referencedClass)) {
          return NEVER;
        }
        return MAYBE;
      }
    }
  }

  @Override
  public ReferenceValue cast(
      String type, Clazz referencedClass, ValueFactory valueFactory, boolean alwaysCast) {
    // Just return this value if it's the same type.
    // Also return this value if it is null or more specific.
    if (!alwaysCast && (this.type == null || instanceOf(type, referencedClass) == ALWAYS)) {
      return this;
    } else if (this.type != null && this.type.equals(type)) {
      return this;
    }
    if (instanceOf(type, referencedClass) == ALWAYS) {
      return valueFactory.createReferenceValue(referencedClass, true, true, objectValue);
    }
    // not instance of this. Returning unknown.
    return valueFactory.createReferenceValue(type, referencedClass, true, true);
  }

  @Override
  public ReferenceValue generalize(ReferenceValue other) {
    return other.generalize(this);
  }

  @Override
  public ReferenceValue generalize(ParticularReferenceValue other) {
    if (this.equal(other) == ALWAYS) {
      // FIXME: This code breaks the semi-lattice properties of Values needed for reliable
      //  convergence in the partial evaluator. Specifically, the generalize operation
      //  should be commutative, but it is not. The following should hold:
      //    a.generalize(b)  ==   b.generalize(a)
      //  Possible solution to this problem could be to deterministically pick one of the two
      //  values and return that one (e.g. the lower ID). Most importantly though, whatever
      //  is implemented here must also be implemented in the IdentifiedReferenceValue's
      //  generalize() method and all possible combinations of Values must be tested.
      //
      //  Note: A fix for the problem was not implemented, because it changes the semantics
      //  of the analysis. Anything that we change here could potentially affect users of PGC.
      return other;
    }

    return super.generalize((IdentifiedReferenceValue) other);
  }

  @Override
  public int hashCode() {
    return this.getClass().hashCode() ^ (objectValue.isNull() ? 1 : objectValue.hashCode());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ParticularReferenceValue that = (ParticularReferenceValue) o;
    return Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int equal(ReferenceValue other) {
    if (this == other) {
      return ALWAYS;
    }
    if (super.equal(other) == NEVER) {
      return NEVER;
    }
    if (getClass() != other.getClass()) {
      return MAYBE;
    }

    ParticularReferenceValue otherParticularValue = (ParticularReferenceValue) other;
    // now, the type and class equals.
    if ((getValue().isNull() && otherParticularValue.getValue().isNull())
        || (!getValue().isNull() && getValue().equals(otherParticularValue.getValue()))) {
      return ALWAYS;
    }
    return NEVER;
  }

  @Override
  public String toString() {
    return super.toString() + "(" + (objectValue == null ? "null" : objectValue.toString()) + ")";
  }
}
