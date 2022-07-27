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
package proguard.classfile.attribute.annotation.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.annotation.*;

/**
 * This interface specifies the methods for a visitor of
 * {@link TypePathInfo} instances. Note that there is only a single
 * implementation of {@link TypePathInfo}, such that this interface
 * is not strictly necessary as a visitor.
 *
 * @author Eric Lafortune
 */
public interface TypePathInfoVisitor
{
    /**
     * Visits any TypePathInfo instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitTypePathInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+typePathInfo.getClass().getName());
    }


    default void visitTypePathInfo(Clazz clazz, Member member, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        visitTypePathInfo(clazz, typeAnnotation, typePathInfo);
    }


    default void visitTypePathInfo(Clazz clazz, Field field, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        visitTypePathInfo(clazz, (Member)field, typeAnnotation, typePathInfo);
    }


    default void visitTypePathInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        visitTypePathInfo(clazz, (Member)method, typeAnnotation, typePathInfo);
    }


    default void visitTypePathInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypePathInfo typePathInfo)
    {
        visitTypePathInfo(clazz, method, typeAnnotation, typePathInfo);
    }
}
