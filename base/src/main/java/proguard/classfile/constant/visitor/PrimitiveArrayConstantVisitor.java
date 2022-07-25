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
 * This interface specifies the methods for a visitor of {@link PrimitiveArrayConstant}
 * instances containing different types of arrays.
 *
 * @author Eric Lafortune
 */
public interface PrimitiveArrayConstantVisitor
{
    /**
     * Visits any Object instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyPrimitiveArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, Object values)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+values.getClass().getName());
    }


    default void visitBooleanArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, boolean[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitByteArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, byte[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitCharArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, char[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitShortArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, short[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitIntArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, int[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitFloatArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, float[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitLongArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, long[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }


    default void visitDoubleArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant, double[] values)
    {
        visitAnyPrimitiveArrayConstant(clazz, primitiveArrayConstant, values);
    }
}
