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
 * This class provides methods to create and reuse Value instances.
 *
 * @author Eric Lafortune
 */
public class BasicValueFactory
implements   ValueFactory
{
    // Shared copies of Value objects, to avoid creating a lot of objects.
    static final IntegerValue   INTEGER_VALUE   = new UnknownIntegerValue();
    static final LongValue      LONG_VALUE      = new UnknownLongValue();
    static final FloatValue     FLOAT_VALUE     = new UnknownFloatValue();
    static final DoubleValue    DOUBLE_VALUE    = new UnknownDoubleValue();
    static final ReferenceValue REFERENCE_VALUE = new UnknownReferenceValue();


    // Implementations for BasicValueFactory.

    public Value createValue(String  type,
                             Clazz   referencedClass,
                             boolean mayBeExtension,
                             boolean mayBeNull)
    {
        switch (type.charAt(0))
        {
            case TypeConstants.VOID:    return null;
            case TypeConstants.BOOLEAN:
            case TypeConstants.BYTE:
            case TypeConstants.CHAR:
            case TypeConstants.SHORT:
            case TypeConstants.INT:     return createIntegerValue();
            case TypeConstants.LONG:    return createLongValue();
            case TypeConstants.FLOAT:   return createFloatValue();
            case TypeConstants.DOUBLE:  return createDoubleValue();
            default:                    return createReferenceValue(type,
                                                                    referencedClass,
                                                                    mayBeExtension,
                                                                    mayBeNull);
        }
    }


    public IntegerValue createIntegerValue()
    {
        return INTEGER_VALUE;
    }


    public IntegerValue createIntegerValue(int value)
    {
        return createIntegerValue();
    }


    public IntegerValue createIntegerValue(int min, int max)
    {
        return createIntegerValue();
    }


    public LongValue createLongValue()
    {
        return LONG_VALUE;
    }


    public LongValue createLongValue(long value)
    {
        return createLongValue();
    }


    public FloatValue createFloatValue()
    {
        return FLOAT_VALUE;
    }


    public FloatValue createFloatValue(float value)
    {
        return createFloatValue();
    }


    public DoubleValue createDoubleValue()
    {
        return DOUBLE_VALUE;
    }


    public DoubleValue createDoubleValue(double value)
    {
        return createDoubleValue();
    }


    public ReferenceValue createReferenceValue()
    {
        return REFERENCE_VALUE;
    }


    public ReferenceValue createReferenceValueNull()
    {
        return REFERENCE_VALUE;
    }


    public ReferenceValue createReferenceValue(String  type,
                                               Clazz   referencedClass,
                                               boolean mayBeExtension,
                                               boolean mayBeNull)
    {
        return createReferenceValue();
    }

    public ReferenceValue createReferenceValue(String  type,
                                               Clazz   referencedClass,
                                               boolean mayBeExtension,
                                               boolean mayBeNull,
                                               Object  value)
    {
        return createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull);
    }

    public ReferenceValue createArrayReferenceValue(String       type,
                                                    Clazz        referencedClass,
                                                    IntegerValue arrayLength)
    {
        return createReferenceValue(type, referencedClass, false, false);
    }


    public ReferenceValue createArrayReferenceValue(String       type,
                                                    Clazz        referencedClass,
                                                    IntegerValue arrayLength,
                                                    Value        elementValue)
    {
        return createArrayReferenceValue(type, referencedClass, arrayLength);
    }
}
