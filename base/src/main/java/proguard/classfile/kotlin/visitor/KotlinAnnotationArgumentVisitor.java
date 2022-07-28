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

package proguard.classfile.kotlin.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.KotlinAnnotatable;
import proguard.classfile.kotlin.KotlinAnnotation;
import proguard.classfile.kotlin.KotlinAnnotationArgument;

import static proguard.classfile.kotlin.KotlinAnnotationArgument.*;

/**
 * Visitor interface for Kotlin annotation arguments.
 *
 * @author James Hamilton
 */
public interface KotlinAnnotationArgumentVisitor
{
    void visitAnyArgument(Clazz                    clazz,
                          KotlinAnnotatable        annotatable,
                          KotlinAnnotation         annotation,
                          KotlinAnnotationArgument argument,
                          Value                    value);


    default void visitAnyLiteralArgument(Clazz                    clazz,
                                         KotlinAnnotatable        annotatable,
                                         KotlinAnnotation         annotation,
                                         KotlinAnnotationArgument argument,
                                         LiteralValue<?>          value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitByteArgument(Clazz                    clazz,
                                   KotlinAnnotatable        annotatable,
                                   KotlinAnnotation         annotation,
                                   KotlinAnnotationArgument argument,
                                   ByteValue                value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitCharArgument(Clazz                    clazz,
                                   KotlinAnnotatable        annotatable,
                                   KotlinAnnotation         annotation,
                                   KotlinAnnotationArgument argument,
                                   CharValue                value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitShortArgument(Clazz                    clazz,
                                    KotlinAnnotatable        annotatable,
                                    KotlinAnnotation         annotation,
                                    KotlinAnnotationArgument argument,
                                    ShortValue               value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitIntArgument(Clazz                    clazz,
                                  KotlinAnnotatable        annotatable,
                                  KotlinAnnotation         annotation,
                                  KotlinAnnotationArgument argument,
                                  IntValue                 value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitLongArgument(Clazz                    clazz,
                                   KotlinAnnotatable        annotatable,
                                   KotlinAnnotation         annotation,
                                   KotlinAnnotationArgument argument,
                                   LongValue                value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitFloatArgument(Clazz                    clazz,
                                    KotlinAnnotatable        annotatable,
                                    KotlinAnnotation         annotation,
                                    KotlinAnnotationArgument argument,
                                    FloatValue               value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitDoubleArgument(Clazz                    clazz,
                                     KotlinAnnotatable        annotatable,
                                     KotlinAnnotation         annotation,
                                     KotlinAnnotationArgument argument,
                                     DoubleValue              value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitBooleanArgument(Clazz                    clazz,
                                      KotlinAnnotatable        annotatable,
                                      KotlinAnnotation         annotation,
                                      KotlinAnnotationArgument argument,
                                      BooleanValue             value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitUByteArgument(Clazz                    clazz,
                                    KotlinAnnotatable        annotatable,
                                    KotlinAnnotation         annotation,
                                    KotlinAnnotationArgument argument,
                                    UByteValue               value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitUShortArgument(Clazz                    clazz,
                                     KotlinAnnotatable        annotatable,
                                     KotlinAnnotation         annotation,
                                     KotlinAnnotationArgument argument,
                                     UShortValue              value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitUIntArgument(Clazz                    clazz,
                                   KotlinAnnotatable        annotatable,
                                   KotlinAnnotation         annotation,
                                   KotlinAnnotationArgument argument,
                                   UIntValue                value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitULongArgument(Clazz                    clazz,
                                    KotlinAnnotatable        annotatable,
                                    KotlinAnnotation         annotation,
                                    KotlinAnnotationArgument argument,
                                    ULongValue               value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitStringArgument(Clazz                    clazz,
                                     KotlinAnnotatable        annotatable,
                                     KotlinAnnotation         annotation,
                                     KotlinAnnotationArgument argument,
                                     StringValue              value)
    {
        visitAnyLiteralArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitClassArgument(Clazz                               clazz,
                                    KotlinAnnotatable                   annotatable,
                                    KotlinAnnotation                    annotation,
                                    KotlinAnnotationArgument            argument,
                                    KotlinAnnotationArgument.ClassValue value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitEnumArgument(Clazz                    clazz,
                                   KotlinAnnotatable        annotatable,
                                   KotlinAnnotation         annotation,
                                   KotlinAnnotationArgument argument,
                                   EnumValue                value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitAnnotationArgument(Clazz                    clazz,
                                         KotlinAnnotatable        annotatable,
                                         KotlinAnnotation         annotation,
                                         KotlinAnnotationArgument argument,
                                         AnnotationValue          value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
    }

    default void visitArrayArgument(Clazz                    clazz,
                                    KotlinAnnotatable        annotatable,
                                    KotlinAnnotation         annotation,
                                    KotlinAnnotationArgument argument,
                                    ArrayValue               value)
    {
        visitAnyArgument(clazz, annotatable, annotation, argument, value);
    }
}
