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
package proguard.classfile.attribute;

import proguard.classfile.*;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.visitor.MemberVisitor;
import proguard.util.SimpleProcessable;

/**
 * Representation of a record component entry.
 *
 * @author Eric Lafortune
 */
public class RecordComponentInfo extends SimpleProcessable
{
    public int         u2nameIndex;
    public int         u2descriptorIndex;
    public int         u2attributesCount;
    public Attribute[] attributes;

    /**
     * An extra field optionally pointing to the referenced Field object.
     * This field is typically filled out by the <code>{@link
     * proguard.classfile.util.ClassReferenceInitializer
     * ClassReferenceInitializer}</code>.
     */
    public Field referencedField;


    /**
     * Creates an uninitialized RecordComponentInfo.
     */
    public RecordComponentInfo()
    {
    }


    /**
     * Creates an initialized RecordComponentInfo.
     */
    public RecordComponentInfo(int         u2nameIndex,
                               int         u2descriptorIndex,
                               int         u2attributesCount,
                               Attribute[] attributes)
    {
        this.u2nameIndex       = u2nameIndex;
        this.u2descriptorIndex = u2descriptorIndex;
        this.u2attributesCount = u2attributesCount;
        this.attributes        = attributes;
    }


    /**
     * Returns the record component descriptor name.
     */
    public String getName(Clazz clazz)
    {
        return clazz.getString(u2nameIndex);
    }

    /**
     * Returns the record component descriptor descriptor.
     */
    public String getDescriptor(Clazz clazz)
    {
        return clazz.getString(u2descriptorIndex);
    }


    /**
     * Lets the referenced class field accept the given visitor.
     */
    public void referencedFieldAccept(Clazz clazz, MemberVisitor memberVisitor)
    {
        if (referencedField != null)
        {
            referencedField.accept(clazz, memberVisitor);
        }
    }


    /**
     * Applies the given attribute visitor to all attributes.
     */
    public void attributesAccept(Clazz clazz, AttributeVisitor attributeVisitor)
    {
        for (int index = 0; index < u2attributesCount; index++)
        {
            attributes[index].accept(clazz, this, attributeVisitor);
        }
    }
}
