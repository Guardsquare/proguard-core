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
package proguard.classfile.attribute.annotation.target;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.annotation.TypeAnnotation;
import proguard.classfile.attribute.annotation.target.visitor.TargetInfoVisitor;

/**
 * Representation of an offset annotation target.
 *
 * @author Eric Lafortune
 */
public class OffsetTargetInfo extends TargetInfo
{
    public int u2offset;


    /**
     * Creates an uninitialized OffsetTargetInfo.
     */
    public OffsetTargetInfo()
    {
    }


    /**
     * Creates a partially initialized OffsetTargetInfo.
     */
    public OffsetTargetInfo(byte u1targetType)
    {
        super(u1targetType);
    }


    /**
     * Creates an initialized OffsetTargetInfo.
     */
    public OffsetTargetInfo(byte u1targetType,
                            int  u2offset)
    {
        super(u1targetType);

        this.u2offset = u2offset;
    }


    // Implementations for TargetInfo.

    /**
     * Lets the visitor visit, with Method and CodeAttribute null.
     */
    public void accept(Clazz clazz, TypeAnnotation typeAnnotation, TargetInfoVisitor targetInfoVisitor)
    {
        targetInfoVisitor.visitOffsetTargetInfo(clazz, null, null, typeAnnotation, this);
    }


    public void accept(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TargetInfoVisitor targetInfoVisitor)
    {
        targetInfoVisitor.visitOffsetTargetInfo(clazz, method, codeAttribute, typeAnnotation, this);
    }
}
