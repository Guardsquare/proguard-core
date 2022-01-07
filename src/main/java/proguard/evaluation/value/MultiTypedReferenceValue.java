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

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import proguard.classfile.Clazz;

/**
 * This {@link TypedReferenceValue} can have multiple potential types during runtime.
 * E.g. when evaluating <code>SuperClass s = someFlag ? new A() : new B()</code>,
 * s may be of type A or B.
 *
 * @author Samuel Hopstock
 */
public class MultiTypedReferenceValue extends ReferenceValue
{

    /**
     * All types that this reference value might possibly have, e.g. {A, B}
     */
    private final Set<TypedReferenceValue> potentialTypes = new HashSet<>();
    /**
     * The most specific supertype of all potential types, calculated by {@link #generalize(Set)}
     * e.g. S for potential types {A, B} if both A and B extend S
     */
    private final TypedReferenceValue      generalizedType;
    public final  boolean                  mayBeUnknown;

    public MultiTypedReferenceValue(Set<TypedReferenceValue> potentialTypes, boolean mayBeUnknown)
    {
        this.mayBeUnknown = mayBeUnknown;
        this.potentialTypes.addAll(potentialTypes);
        generalizedType = generalize(potentialTypes);
    }

    public MultiTypedReferenceValue(TypedReferenceValue type, boolean mayBeUnknown)
    {
        this.mayBeUnknown = mayBeUnknown;
        potentialTypes.add(type);
        generalizedType = type;
    }

    private TypedReferenceValue checkForAlreadyContainedType(TypedReferenceValue newGeneralizedType)
    {
        // If we have the generalized type already in our potential types, use this one instead.
        // Even if this new type might have different values for "mayBeExtension" or "mayBeNull"
        Optional<TypedReferenceValue> matchingPotentialType = potentialTypes.stream()
                                                                            .filter(t -> Objects.equals(t.getType(), newGeneralizedType.getType()) &&
                                                                                         Objects.equals(t.getReferencedClass(), newGeneralizedType.getReferencedClass()))
                                                                            .findAny();
        return matchingPotentialType.orElse(newGeneralizedType);
    }

    public TypedReferenceValue generalize(Set<TypedReferenceValue> potentialTypes)
    {
        TypedReferenceValue generalizedType = null;
        for (TypedReferenceValue type : potentialTypes)
        {
            if (generalizedType == null)
            {
                generalizedType = type;
            }
            else
            {
                ReferenceValue newGeneralizedType = generalizedType.generalize(type);
                if (newGeneralizedType instanceof TypedReferenceValue)
                {
                    generalizedType = (TypedReferenceValue) newGeneralizedType;
                }
                else
                {
                    throw new IllegalStateException("Generalized type not a typed reference value: " + newGeneralizedType.getClass().getSimpleName());
                }
            }
        }

        return checkForAlreadyContainedType(generalizedType);
    }

    public Set<TypedReferenceValue> getPotentialTypes()
    {
        return potentialTypes;
    }

    public TypedReferenceValue getGeneralizedType()
    {
        return generalizedType;
    }

    private int conditionMatches(Set<Integer> possibilities)
    {
        if (possibilities.size() == 1)
        {
            return possibilities.iterator().next();
        }
        return MAYBE;
    }

    @Override
    public String getType()
    {
        return generalizedType.getType();
    }

    @Override
    public Clazz getReferencedClass()
    {
        return generalizedType.getReferencedClass();
    }

    @Override
    public boolean mayBeExtension()
    {
        return potentialTypes.stream().anyMatch(TypedReferenceValue::mayBeExtension);
    }

    @Override
    public int isNull()
    {
        return conditionMatches(potentialTypes.stream()
                                              .map(TypedReferenceValue::isNull)
                                              .collect(Collectors.toSet()));
    }

    @Override
    public int instanceOf(String otherType, Clazz otherReferencedClass)
    {
        return conditionMatches(potentialTypes.stream().map(t -> t.instanceOf(otherType, otherReferencedClass)).collect(Collectors.toSet()));
    }

    @Override
    public ReferenceValue cast(String type, Clazz referencedClass, ValueFactory valueFactory, boolean alwaysCast)
    {
        if (instanceOf(type, referencedClass) == ALWAYS)
        {
            return this;
        }

        return new MultiTypedReferenceValue(new TypedReferenceValue(type, referencedClass, mayBeExtension(), isNull() != NEVER), mayBeUnknown);
    }

    @Override
    public ReferenceValue referenceArrayLoad(IntegerValue indexValue, ValueFactory valueFactory)
    {
        Set<TypedReferenceValue> potentialTypes = this.potentialTypes.stream()
                                                                     .map(t -> t.referenceArrayLoad(indexValue, valueFactory))
                                                                     .filter(MultiTypedReferenceValue.class::isInstance)
                                                                     .map(MultiTypedReferenceValue.class::cast)
                                                                     .flatMap(t -> t.potentialTypes.stream())
                                                                     .collect(Collectors.toSet());

        if (potentialTypes.isEmpty())
        {
            return valueFactory.createReferenceValue();
        }

        return new MultiTypedReferenceValue(potentialTypes, mayBeUnknown);
    }

    @Override
    public ReferenceValue generalize(ReferenceValue other)
    {
        return other.generalize(this);
    }

    @Override
    public ReferenceValue generalize(TypedReferenceValue other)
    {
        // Transparently handle this case
        return generalize(new MultiTypedReferenceValue(other, false));
    }

    @Override
    public ReferenceValue generalize(UnknownReferenceValue other)
    {
        return new MultiTypedReferenceValue(potentialTypes, true);
    }

    @Override
    public ReferenceValue generalize(MultiTypedReferenceValue other)
    {
        if (this.equals(other))
        {
            return this;
        }

        Set<TypedReferenceValue> newPotentialTypes = new HashSet<>(this.potentialTypes);
        newPotentialTypes.addAll(other.potentialTypes);
        return new MultiTypedReferenceValue(newPotentialTypes, mayBeUnknown || other.mayBeUnknown);
    }

    @Override
    public int equal(ReferenceValue other)
    {
        return other.equal(this);
    }

    @Override
    public int equal(MultiTypedReferenceValue other)
    {
        return conditionMatches(potentialTypes.stream()
                                              .map(t -> t.equal(other.generalizedType))
                                              .collect(Collectors.toSet()));
    }

    @Override
    public String internalType()
    {
        return generalizedType.internalType();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }
        MultiTypedReferenceValue that = (MultiTypedReferenceValue) o;
        return Objects.equals(potentialTypes, that.potentialTypes)
               && mayBeUnknown == that.mayBeUnknown;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), potentialTypes);
    }

    @Override
    public String toString()
    {
        return "potentialTypes=[" + potentialTypes.stream().map(TypedReferenceValue::toString).collect(Collectors.joining(", ")) +
               "], generalizedType=" + generalizedType;
    }
}
