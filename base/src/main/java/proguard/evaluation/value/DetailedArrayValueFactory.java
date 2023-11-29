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
package proguard.evaluation.value;

import proguard.classfile.*;

/**
 * This identified value factory creates array reference values that also
 * represent their elements, in as far as possible.
 *
 * @author Eric Lafortune
 */
public class DetailedArrayValueFactory
extends      IdentifiedValueFactory
{
    // Implementations for ReferenceValue.

    public ReferenceValue createArrayReferenceValue(String       type,
                                                    Clazz        referencedClass,
                                                    IntegerValue arrayLength)
    {
        return type == null ?
               TypedReferenceValueFactory.REFERENCE_VALUE_NULL :
               arrayLength.isParticular() ?
               new DetailedArrayReferenceValue(TypeConstants.ARRAY + type,
                                               referencedClass,
                                               false,
                                               arrayLength,
                                               this,
                                               referenceID++) :
               new IdentifiedArrayReferenceValue(TypeConstants.ARRAY + type,
                                                 referencedClass,
                                                 false,
                                                 arrayLength,
                                                 this,
                                                 referenceID++);
    }

    public ReferenceValue createArrayReferenceValue(String       type,
                                                    Clazz        referencedClass,
                                                    IntegerValue arrayLength,
                                                    Object       elementValues)

    {


        if (type == null) return TypedReferenceValueFactory.REFERENCE_VALUE_NULL;

        if (!arrayLength.isParticular())
        {
            return new IdentifiedArrayReferenceValue(type,
                                                      referencedClass,
                                                      false,
                                                      arrayLength,
                                                      this,
                                                      referenceID++);
        }
        if(!elementValues.getClass().isArray() || elementValues.getClass().getComponentType().isArray()){
            throw new IllegalArgumentException("Only one-dimension array type is supported: " + elementValues.getClass());
        }


        DetailedArrayReferenceValue detailedArray = new DetailedArrayReferenceValue(type,
                                                                                    referencedClass,
                                                                                    false,
                                                                                    arrayLength,
                                                                                    this,
                                                                                    referenceID++);
        if(elementValues.getClass().isArray())
        {
            switch (type.charAt(1))// 0 is the array char
            {
                case TypeConstants.BOOLEAN: storeBooleanArray(detailedArray, (boolean[]) elementValues); break;
                case TypeConstants.BYTE:    storeByteArray(detailedArray, (byte[]) elementValues);       break;
                case TypeConstants.CHAR:    storeCharArray(detailedArray, (char[]) elementValues);       break;
                case TypeConstants.SHORT:   storeShortArray(detailedArray, (short[]) elementValues);     break;
                case TypeConstants.INT:     storeIntArray(detailedArray, (int[]) elementValues);         break;
                case TypeConstants.LONG:    storeLongArray(detailedArray, (long[]) elementValues);       break;
                case TypeConstants.FLOAT:   storeFloatArray(detailedArray, (float[]) elementValues);     break;
                case TypeConstants.DOUBLE:  storeDoubleArray(detailedArray, (double[]) elementValues);   break;
                default:                    storeObjectArray(detailedArray, (Object[]) elementValues);
            }
        }
        return detailedArray;
    }

    private void storeBooleanArray(DetailedArrayReferenceValue detailedArray, boolean[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i] ? 1:0));
        }
    }

    private void storeByteArray(DetailedArrayReferenceValue detailedArray, byte[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
        }
    }

    private void storeCharArray(DetailedArrayReferenceValue detailedArray, char[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
        }
    }

    private void storeShortArray(DetailedArrayReferenceValue detailedArray, short[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
        }
    }

    private void storeIntArray(DetailedArrayReferenceValue detailedArray, int[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createIntegerValue(elementValues[i]));
        }
    }

    private void storeLongArray(DetailedArrayReferenceValue detailedArray, long[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createLongValue(elementValues[i]));
        }
    }


    private void storeFloatArray(DetailedArrayReferenceValue detailedArray, float[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createFloatValue(elementValues[i]));
        }
    }


    private void storeDoubleArray(DetailedArrayReferenceValue detailedArray, double[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createDoubleValue(elementValues[i]));
        }
    }

    private void storeObjectArray(DetailedArrayReferenceValue detailedArray, Object[] elementValues)
    {
        for (int i = 0; i < elementValues.length; i++)
        {
            detailedArray.arrayStore(createIntegerValue(i), createReferenceValue(detailedArray.referencedClass, elementValues[i]));
        }
    }
}
