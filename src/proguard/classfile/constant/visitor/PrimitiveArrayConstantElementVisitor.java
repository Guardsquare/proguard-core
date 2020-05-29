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
package proguard.classfile.constant.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.constant.PrimitiveArrayConstant;

/**
 * This interface specifies the methods for a visitor of primitive elements
 * of the array of a {@link PrimitiveArrayConstant}.
 *
 * @author Eric Lafortune
 */
public interface PrimitiveArrayConstantElementVisitor
{
    /**
     * Visits any PrimitiveArrayConstant instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyPrimitiveArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+primitiveArrayConstant.values.getClass().getName());
    }


    default void visitBooleanArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, boolean value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitByteArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, byte value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitCharArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, char value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitShortArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, short value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitIntArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, int value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitFloatArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, float value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitLongArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, long value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }


    default void visitDoubleArrayConstantElement(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int index, double value)
    {
        visitAnyPrimitiveArrayConstantElement(clazz, primitiveArrayConstant, index);
    }
}
