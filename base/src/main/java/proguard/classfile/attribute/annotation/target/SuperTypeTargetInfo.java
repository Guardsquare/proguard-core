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

import proguard.classfile.Clazz;
import proguard.classfile.attribute.annotation.TypeAnnotation;
import proguard.classfile.attribute.annotation.target.visitor.TargetInfoVisitor;

/**
 * Representation of a super type annotation target.
 *
 * @author Eric Lafortune
 */
public class SuperTypeTargetInfo extends TargetInfo
{
    public static final int EXTENDS_INDEX = 65535;


    public int u2superTypeIndex;


    /**
     * Creates an uninitialized SuperTypeTargetInfo.
     */
    public SuperTypeTargetInfo()
    {
    }


    /**
     * Creates a partially initialized SuperTypeTargetInfo.
     */
    public SuperTypeTargetInfo(byte u1targetType)
    {
        super(u1targetType);
    }


    /**
     * Creates an initialized SuperTypeTargetInfo.
     */
    public SuperTypeTargetInfo(byte u1targetType,
                               int  u2superTypeIndex)
    {
        super(u1targetType);

        this.u2superTypeIndex = u2superTypeIndex;
    }


    // Implementations for TargetInfo.

    public void accept(Clazz clazz, TypeAnnotation typeAnnotation, TargetInfoVisitor targetInfoVisitor)
    {
        targetInfoVisitor.visitSuperTypeTargetInfo(clazz, typeAnnotation, this);
    }
}
