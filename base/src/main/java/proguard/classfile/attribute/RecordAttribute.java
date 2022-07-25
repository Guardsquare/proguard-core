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

import proguard.classfile.Clazz;
import proguard.classfile.attribute.visitor.*;

/**
 * This {@link Attribute} represents a record attribute.
 *
 * @author Eric Lafortune
 */
public class RecordAttribute extends Attribute
{
    public int                   u2componentsCount;
    public RecordComponentInfo[] components;


    /**
     * Creates an uninitialized RecordAttribute.
     */
    public RecordAttribute()
    {
    }


    /**
     * Creates an initialized RecordAttribute.
     */
    public RecordAttribute(int                   u2attributeNameIndex,
                           int                   u2componentsCount,
                           RecordComponentInfo[] components)
    {
        super(u2attributeNameIndex);

        this.u2componentsCount = u2componentsCount;
        this.components        = components;
    }


    // Implementations for Attribute.

    public void accept(Clazz clazz, AttributeVisitor attributeVisitor)
    {
        attributeVisitor.visitRecordAttribute(clazz, this);
    }


    /**
     * Applies the given visitor to all components.
     */
    public void componentsAccept(Clazz clazz, RecordComponentInfoVisitor recordComponentInfoVisitor)
    {
        for (int index = 0; index < u2componentsCount; index++)
        {
            // We don't need double dispatching here, since there is only one
            // type of RecordComponentInfo.
            recordComponentInfoVisitor.visitRecordComponentInfo(clazz, components[index]);
        }
    }
}
