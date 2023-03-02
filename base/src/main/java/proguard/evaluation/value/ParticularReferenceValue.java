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

import proguard.classfile.AccessConstants;
import proguard.classfile.Clazz;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassCounter;
import proguard.classfile.visitor.ClassNameFilter;

import java.util.Objects;

/**
 * This {@link ParticularReferenceValue} represents a particular reference value, i.e. a reference with an associated value.
 * E.g., a String with the value "HelloWorld".
 *
 * @author Dennis Titze
 */
public class ParticularReferenceValue extends IdentifiedReferenceValue
{

    // The actual value of the object.
    private final Object value;

    public static final Object UNINITIALIZED = new Object()
    {
        @Override
        public String toString()
        {
            return "UNINITIALIZED";
        }
    };


    /**
     * Create a new Instance with the given type, the class it is referenced in, and its actual value.
     */
    public ParticularReferenceValue(String       type,
                                    Clazz        referencedClass,
                                    ValueFactory valueFactory,
                                    Object       referenceID,
                                    Object       value)
    {
        // We store the unique ID to keep track of the same value (independent of casting and generalizations) on stack and vars.
        // This ID is needed, since the generalization and casting might create new instances, and we need to see that these were in fact the ones we need to replace.
        super(type, referencedClass, false, true, valueFactory, referenceID);

        this.value = value;

        ClassCounter counter = new ClassCounter();
        if (value != UNINITIALIZED && value != null && referencedClass != null)
        {
            referencedClass.hierarchyAccept(
                true,
                false,
                false,
                true,
                // if the value is an array check the inheritance for the referenced type
                new ClassNameFilter(ClassUtil.internalClassName(ClassUtil.externalBaseType(value.getClass().getCanonicalName())), counter)
            );
        }
        boolean isExtended = counter.getCount() > 0;

        // Sanity checks.
        // If referenced class is known we can check for inheritance
        // If referenced class is unknown the best we can do is checking if the type exactly matches the value type
        if (value != UNINITIALIZED &&
            value != null
            && (referencedClass != null && !isExtended
                || referencedClass == null && !type.equals(ClassUtil.internalType(value.getClass().getCanonicalName()))))
        {
            throw new RuntimeException("Type does not match or is not extended by type of the value (" + ClassUtil.internalType(value.getClass().getCanonicalName()) + " - " + type + ")");
        }
        if (type == null)
        {
            throw new RuntimeException("Type must not be null");
        }
    }

    // Implementations for ReferenceValue.

    @Override
    public Object value()
    {
        return value;
    }

    // Implementations for TypedReferenceValue.

    @Override
    public boolean isParticular()
    {
        return true;
    }

    @Override
    public int isNull()
    {
        return value == null ? ALWAYS : NEVER;
    }

    @Override
    public int instanceOf(String otherType, Clazz otherReferencedClass)
    {
        // If the value extends the type, we're sure (unless it may be null).
        // Otherwise, if the value type is final, it can never be an instance.
        // Also, if the types are not interfaces and not in the same hierarchy,
        // the value can never be an instance.
        if (referencedClass == null ||
            otherReferencedClass == null)
        {
            return MAYBE;
        }
        else
        {
            if (referencedClass.extendsOrImplements(otherReferencedClass))
            {
                return ALWAYS;
            }
            else
            {
                if ((referencedClass.getAccessFlags() & AccessConstants.FINAL) != 0)
                {
                    return NEVER;
                }
                if ((referencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0 &&
                    (otherReferencedClass.getAccessFlags() & AccessConstants.INTERFACE) == 0 &&
                    !otherReferencedClass.extendsOrImplements(referencedClass))
                {
                    return NEVER;
                }
                return MAYBE;
            }
        }
    }

    @Override
    public ReferenceValue cast(String type, Clazz referencedClass, ValueFactory valueFactory, boolean alwaysCast)
    {
        // Just return this value if it's the same type.
        // Also return this value if it is null or more specific.
        if (!alwaysCast &&
            (this.type == null ||
             instanceOf(type, referencedClass) == ALWAYS))
        {
            return this;
        }
        else if (this.type != null &&
                 this.type.equals(type))
        {
            return this;
        }
        if (instanceOf(type, referencedClass) == ALWAYS)
        {
            return valueFactory.createReferenceValue(type,
                                                     referencedClass,
                                                     true,
                                                     true,
                                                     value);
        }
        // not instance of this. Returning unknown.
        return valueFactory.createReferenceValue(type,
                                                 referencedClass,
                                                 true,
                                                 true);
    }

    @Override
    public ReferenceValue generalize(ReferenceValue other)
    {
        return other.generalize(this);
    }

    @Override
    public ReferenceValue generalize(ParticularReferenceValue other)
    {
        if (this.equal(other) == ALWAYS)
        {
            return other;
        }

        return super.generalize((IdentifiedReferenceValue) other);
    }

    @Override
    public int hashCode()
    {
        return this.getClass().hashCode() ^
               (value == null ? 1 : value.hashCode());
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ParticularReferenceValue that = (ParticularReferenceValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int equal(ReferenceValue other)
    {
        if (this == other)
        {
            return ALWAYS;
        }
        if (super.equal(other) == NEVER)
        {
            return NEVER;
        }
        if (getClass() != other.getClass())
        {
            return MAYBE;
        }
        // now, the type and class equals.
        if ((value == null && other.value() == null)
            ||
            (value != null && value.equals(other.value())))
        {
            return ALWAYS;
        }
        return NEVER;

    }

    @Override
    public String toString()
    {
        return super.toString() + "(" + (value == null ? "null" : value.toString()) + ")";
    }

}
