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
package proguard.classfile.attribute.preverification.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.preverification.*;

/**
 * This interface specifies the methods for a visitor of
 * {@link VerificationType} instances. There a methods for stack entries
 * and methods for variable entries.
 *
 * @author Eric Lafortune
 */
public interface VerificationTypeVisitor
{
    /**
     * Visits any VerificationType instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyVerificationType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VerificationType verificationType)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+verificationType.getClass().getName());
    }


    default void visitIntegerType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, IntegerType integerType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, integerType);
    }


    default void visitFloatType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, FloatType floatType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, floatType);
    }


    default void visitLongType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, LongType longType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, longType);
    }


    default void visitDoubleType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, DoubleType doubleType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, doubleType);
    }


    default void visitTopType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, TopType topType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, topType);
    }


    default void visitObjectType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ObjectType objectType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, objectType);
    }


    default void visitNullType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, NullType nullType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, nullType);
    }


    default void visitUninitializedType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, UninitializedType uninitializedType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, uninitializedType);
    }


    default void visitUninitializedThisType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, UninitializedThisType uninitializedThisType)
    {
        visitAnyVerificationType(clazz, method, codeAttribute, offset, uninitializedThisType);
    }


    default void visitStackIntegerType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, IntegerType integerType)
    {
        visitIntegerType(clazz, method, codeAttribute, offset, integerType);
    }


    default void visitStackFloatType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, FloatType floatType)
    {
        visitFloatType(clazz, method, codeAttribute, offset, floatType);
    }


    default void visitStackLongType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, LongType longType)
    {
        visitLongType(clazz, method, codeAttribute, offset, longType);
    }


    default void visitStackDoubleType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, DoubleType doubleType)
    {
        visitDoubleType(clazz, method, codeAttribute, offset, doubleType);
    }


    default void visitStackTopType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, TopType topType)
    {
        visitTopType(clazz, method, codeAttribute, offset, topType);
    }


    default void visitStackObjectType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, ObjectType objectType)
    {
        visitObjectType(clazz, method, codeAttribute, offset, objectType);
    }


    default void visitStackNullType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, NullType nullType)
    {
        visitNullType(clazz, method, codeAttribute, offset, nullType);
    }


    default void visitStackUninitializedType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, UninitializedType uninitializedType)
    {
        visitUninitializedType(clazz, method, codeAttribute, offset, uninitializedType);
    }


    default void visitStackUninitializedThisType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, UninitializedThisType uninitializedThisType)
    {
        visitUninitializedThisType(clazz, method, codeAttribute, offset, uninitializedThisType);
    }


    default void visitVariablesIntegerType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, IntegerType integerType)
    {
        visitIntegerType(clazz, method, codeAttribute, offset, integerType);
    }


    default void visitVariablesFloatType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, FloatType floatType)
    {
        visitFloatType(clazz, method, codeAttribute, offset, floatType);
    }


    default void visitVariablesLongType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, LongType longType)
    {
        visitLongType(clazz, method, codeAttribute, offset, longType);
    }


    default void visitVariablesDoubleType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, DoubleType doubleType)
    {
        visitDoubleType(clazz, method, codeAttribute, offset, doubleType);
    }


    default void visitVariablesTopType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, TopType topType)
    {
        visitTopType(clazz, method, codeAttribute, offset, topType);
    }


    default void visitVariablesObjectType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, ObjectType objectType)
    {
        visitObjectType(clazz, method, codeAttribute, offset, objectType);
    }


    default void visitVariablesNullType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, NullType nullType)
    {
        visitNullType(clazz, method, codeAttribute, offset, nullType);
    }


    default void visitVariablesUninitializedType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, UninitializedType uninitializedType)
    {
        visitUninitializedType(clazz, method, codeAttribute, offset, uninitializedType);
    }


    default void visitVariablesUninitializedThisType(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, int index, UninitializedThisType uninitializedThisType)
    {
        visitUninitializedThisType(clazz, method, codeAttribute, offset, uninitializedThisType);
    }
}
