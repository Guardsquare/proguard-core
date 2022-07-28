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
import proguard.classfile.attribute.annotation.Annotation;

/**
 * This interface specifies the methods for a visitor of
 * {@link Annotation} instances. Note that there is only a single
 * implementation of {@link Annotation}, such that this interface
 * is not strictly necessary as a visitor.
 *
 * @author Eric Lafortune
 */
public interface AnnotationVisitor
{
    /**
     * Visits any Annotation instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+annotation.getClass().getName());
    }



    default void visitAnnotation(Clazz clazz, Member member, Annotation annotation)
    {
        visitAnnotation(clazz, annotation);
    }


    default void visitAnnotation(Clazz clazz, Field field, Annotation annotation)
    {
        visitAnnotation(clazz, (Member)field, annotation);
    }


    default void visitAnnotation(Clazz clazz, Method method, Annotation annotation)
    {
        visitAnnotation(clazz, (Member)method, annotation);
    }


    default void visitAnnotation(Clazz clazz, Method method, int parameterIndex, Annotation annotation)
    {
        visitAnnotation(clazz, method, annotation);
    }


    default void visitAnnotation(Clazz clazz, Method method, CodeAttribute codeAttribute, Annotation annotation)
    {
        visitAnnotation(clazz, method, annotation);
    }
}
