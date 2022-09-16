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
package proguard.util;

import proguard.classfile.*;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.Constant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.visitor.*;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.visitor.ResourceFileVisitor;

/**
 *  This interface defines visitor methods for the main Processable implementations.
 */
public interface ProcessableVisitor
extends          ClassVisitor,
                 MemberVisitor,
                 AttributeVisitor,
                 ResourceFileVisitor,
                 ConstantVisitor
{
    /**
     * Visits any Processable instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyProcessable(Processable processable)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+processable.getClass().getName());
    }


    // Implementations for ClassVisitor.

    default void visitAnyClass(Clazz clazz)
    {
        visitAnyProcessable(clazz);
    }


    // Implementations for MemberVisitor.

    default void visitAnyMember(Clazz clazz, Member member)
    {
        visitAnyProcessable(member);
    }


    // Implementations for AttributeVisitor.

    default void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
        visitAnyProcessable(attribute);
    }


    // Implementations for ResourceFileVisitor.

    default void visitAnyResourceFile(ResourceFile resourceFile)
    {
        visitAnyProcessable(resourceFile);
    }

    // Implementations for ConstantVisitor.

    default void visitAnyConstant(Clazz clazz, Constant constant)
    {
        visitAnyProcessable(constant);
    }
}
