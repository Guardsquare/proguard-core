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

/**
 * This ValueFactory provides methods to create and reuse IntegerValue instances
 * that have known ranges.
 *
 * This implementation creates RangeIntegerValue instances in all IntegerValue
 * factory methods. This way, the RangeIntegerValue instances can generalize
 * further to other RangeIntegerValue instances, even if they start out as known
 * particular values.
 *
 * @author Eric Lafortune
 */
public class RangeValueFactory
extends      BasicRangeValueFactory
implements   ValueFactory
{
    // Shared copies of Value objects, to avoid creating a lot of objects.
    static final IntegerValue INTEGER_VALUE_BYTE  = new RangeIntegerValue(Byte.MIN_VALUE,      Byte.MAX_VALUE);
    static final IntegerValue INTEGER_VALUE_CHAR  = new RangeIntegerValue(Character.MIN_VALUE, Character.MAX_VALUE);
    static final IntegerValue INTEGER_VALUE_SHORT = new RangeIntegerValue(Short.MIN_VALUE,     Short.MAX_VALUE);
    static final IntegerValue INTEGER_VALUE_INT   = new RangeIntegerValue(Integer.MIN_VALUE,   Integer.MAX_VALUE);


    /**
     * Creates a new RangeValueFactory.
     */
    public RangeValueFactory()
    {
        super();
    }


    /**
     * Creates a new RangeValueFactory that delegates to the given
     * value factories for creating reference values.
     */
    public RangeValueFactory(ValueFactory arrayReferenceValueFactory, ValueFactory referenceValueFactory)
    {
        super(arrayReferenceValueFactory, referenceValueFactory);
    }


    // Implementations for ValueFactory.

    public IntegerValue createIntegerValue()
    {
        return INTEGER_VALUE_INT;
    }


    public IntegerValue createIntegerValue(int value)
    {
        return new RangeIntegerValue(value, value);
    }


    public IntegerValue createIntegerValue(int min, int max)
    {
        return new RangeIntegerValue(min, max);
    }
}
