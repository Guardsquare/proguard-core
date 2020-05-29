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

package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.ConstantValueAttribute;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This AttributeVisitor lets a given ConstantVisitor visit all constants
 * of the constant value attributes it visits.
 *
 * @author Eric Lafortune
 */
public class AttributeConstantVisitor
implements AttributeVisitor
{
    private final ConstantVisitor constantVisitor;


    /**
     * Creates a new InstructionConstantVisitor.
     * @param constantVisitor the ConstantVisitor to which visits will be
     *                        delegated.
     */
    public AttributeConstantVisitor(ConstantVisitor constantVisitor)
    {
        this.constantVisitor = constantVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        clazz.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex,
                                      constantVisitor);
    }
}
