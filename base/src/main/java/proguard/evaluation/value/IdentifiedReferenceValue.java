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

import proguard.classfile.Clazz;

/**
 * This {@link ReferenceValue} represents a reference value that is identified by a
 * unique ID.
 *
 * @author Eric Lafortune
 */
public class IdentifiedReferenceValue extends TypedReferenceValue
{
    private final ValueFactory valuefactory;
    public final  int          id;


    /**
     * Creates a new reference value with the given ID.
     */
    public IdentifiedReferenceValue(String       type,
                                    Clazz        referencedClass,
                                    boolean      mayBeExtension,
                                    boolean      mayBeNull,
                                    ValueFactory valuefactory,
                                    int          id)
    {
        super(type, referencedClass, mayBeExtension, mayBeNull);

        this.valuefactory = valuefactory;
        this.id           = id;
    }


    // Implementations of binary methods of ReferenceValue.

    public ReferenceValue generalize(ReferenceValue other)
    {
        return other.generalize(this);
    }


    public int equal(ReferenceValue other)
    {
        return other.equal(this);
    }


//    // Implementations of binary ReferenceValue methods with
//    // UnknownReferenceValue arguments.
//
//    public ReferenceValue generalize(UnknownReferenceValue other)
//    {
//        return other;
//    }
//
//
//    public int equal(UnknownReferenceValue other)
//    {
//        return MAYBE;
//    }
//
//
//    // Implementations of binary ReferenceValue methods with
//    // TypedReferenceValue arguments.
//
//    public ReferenceValue generalize(TypedReferenceValue other)
//    {
//    }
//
//
//    public int equal(TypedReferenceValue other)
//    {
//    }


    // Implementations of binary ReferenceValue methods with
    // IdentifiedReferenceValue arguments.

//    public ReferenceValue generalize(IdentifiedReferenceValue other)
//    {
//        return generalize((TypedReferenceValue)other);
//    }


    public int equal(IdentifiedReferenceValue other)
    {
        return this.equals(other) ? ALWAYS :
                                    this.equal((TypedReferenceValue)other);
    }


//    // Implementations of binary ReferenceValue methods with
//    // ArrayReferenceValue arguments.
//
//    public ReferenceValue generalize(ArrayReferenceValue other)
//    {
//        return generalize((TypedReferenceValue)other);
//    }
//
//
//    public int equal(ArrayReferenceValue other)
//    {
//        return equal((TypedReferenceValue)other);
//    }
//
//
//    // Implementations of binary ReferenceValue methods with
//    // IdentifiedArrayReferenceValue arguments.
//
//    public ReferenceValue generalize(IdentifiedArrayReferenceValue other)
//    {
//        return generalize((ArrayReferenceValue)other);
//    }
//
//
//    public int equal(IdentifiedArrayReferenceValue other)
//    {
//        return equal((ArrayReferenceValue)other);
//    }
//
//
//    // Implementations of binary ReferenceValue methods with
//    // DetailedArrayReferenceValue arguments.
//
//    public ReferenceValue generalize(DetailedArrayReferenceValue other)
//    {
//        return generalize((IdentifiedArrayReferenceValue)other);
//    }
//
//
//    public int equal(DetailedArrayReferenceValue other)
//    {
//        return equal((IdentifiedArrayReferenceValue)other);
//    }
//
//
//    // Implementations of binary ReferenceValue methods with
//    // TracedReferenceValue arguments.
//
//    public ReferenceValue generalize(TracedReferenceValue other)
//    {
//        return other.generalize(this);
//    }
//
//
//    public int equal(TracedReferenceValue other)
//    {
//        return other.equal(this);
//    }


    // Implementations for Value.

    public boolean isSpecific()
    {
        return true;
    }


    // Implementations for Object.

    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }

        if (!super.equals(object))
        {
            return false;
        }

        IdentifiedReferenceValue other = (IdentifiedReferenceValue)object;
        return this.valuefactory.equals(other.valuefactory) &&
               this.id == other.id;
    }


    public int hashCode()
    {
        return super.hashCode() ^
               valuefactory.hashCode() ^
               id;
    }


    public String toString()
    {
        return super.toString()+'#'+id;
    }
}
