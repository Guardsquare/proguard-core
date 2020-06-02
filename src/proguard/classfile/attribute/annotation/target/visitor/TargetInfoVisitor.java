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
package proguard.classfile.attribute.annotation.target.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.target.*;

/**
 * This interface specifies the methods for a visitor of {@link TargetInfo}
 * instances.
 *
 * @author Eric Lafortune
 */
public interface TargetInfoVisitor
{
    /**
     * Visits any TargetInfo instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TargetInfo targetInfo)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+targetInfo.getClass().getName());
    }


    default void visitTypeParameterTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterTargetInfo typeParameterTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, typeParameterTargetInfo);
    }


    default void visitTypeParameterTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, TypeParameterTargetInfo typeParameterTargetInfo)
    {
        visitTypeParameterTargetInfo(clazz, typeAnnotation, typeParameterTargetInfo);
    }


    default void visitSuperTypeTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, SuperTypeTargetInfo superTypeTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, superTypeTargetInfo);
    }


    default void visitTypeParameterBoundTargetInfo(Clazz clazz, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, typeParameterBoundTargetInfo);
    }


    default void visitTypeParameterBoundTargetInfo(Clazz clazz, Member member, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        visitTypeParameterBoundTargetInfo(clazz, typeAnnotation, typeParameterBoundTargetInfo);
    }


    default void visitTypeParameterBoundTargetInfo(Clazz clazz, Field field, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        visitTypeParameterBoundTargetInfo(clazz, (Member)field, typeAnnotation, typeParameterBoundTargetInfo);
    }


    default void visitTypeParameterBoundTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, TypeParameterBoundTargetInfo typeParameterBoundTargetInfo)
    {
        visitTypeParameterBoundTargetInfo(clazz, (Member)method, typeAnnotation, typeParameterBoundTargetInfo);
    }


    default void visitEmptyTargetInfo(Clazz clazz, Member member, TypeAnnotation typeAnnotation, EmptyTargetInfo emptyTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, emptyTargetInfo);
    }


    default void visitEmptyTargetInfo(Clazz clazz, Field field, TypeAnnotation typeAnnotation, EmptyTargetInfo emptyTargetInfo)
    {
        visitEmptyTargetInfo(clazz, (Member)field, typeAnnotation, emptyTargetInfo);
    }


    default void visitEmptyTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, EmptyTargetInfo emptyTargetInfo)
    {
        visitEmptyTargetInfo(clazz, (Member)method, typeAnnotation, emptyTargetInfo);
    }


    default void visitFormalParameterTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, FormalParameterTargetInfo formalParameterTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, formalParameterTargetInfo);
    }


    default void visitThrowsTargetInfo(Clazz clazz, Method method, TypeAnnotation typeAnnotation, ThrowsTargetInfo throwsTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, throwsTargetInfo);
    }


    default void visitLocalVariableTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, LocalVariableTargetInfo localVariableTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, localVariableTargetInfo);
    }


    default void visitCatchTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, CatchTargetInfo catchTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, catchTargetInfo);
    }


    default void visitOffsetTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, OffsetTargetInfo offsetTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, offsetTargetInfo);
    }


    default void visitTypeArgumentTargetInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, TypeAnnotation typeAnnotation, TypeArgumentTargetInfo typeArgumentTargetInfo)
    {
        visitAnyTargetInfo(clazz, typeAnnotation, typeArgumentTargetInfo);
    }
}
