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
 * This basic implementation only creates RangeIntegerValue instances if they
 * start out with a known range. Otherwise, it still creates
 * ParticularIntegerValue instances or UnknownIntegerValue instances, which by
 * themselves never generalize to RangeIntegerValue instances.
 *
 * @author Eric Lafortune
 */
public class BasicRangeValueFactory
extends      ParticularValueFactory
implements   ValueFactory
{
    // Shared copies of Value objects, to avoid creating a lot of objects.
    static final IntegerValue INTEGER_VALUE_BYTE  = new RangeIntegerValue(Byte.MIN_VALUE,      Byte.MAX_VALUE);
    static final IntegerValue INTEGER_VALUE_CHAR  = new RangeIntegerValue(Character.MIN_VALUE, Character.MAX_VALUE);
    static final IntegerValue INTEGER_VALUE_SHORT = new RangeIntegerValue(Short.MIN_VALUE,     Short.MAX_VALUE);


    /**
     * Creates a new BasicRangeValueFactory.
     */
    public BasicRangeValueFactory()
    {
        super();
    }


    /**
     * Creates a new BasicRangeValueFactory that delegates to the given
     * value factories for creating reference values.
     */
    public BasicRangeValueFactory(ValueFactory arrayReferenceValueFactory, ValueFactory referenceValueFactory)
    {
        super(arrayReferenceValueFactory, referenceValueFactory);
    }


    // Implementations for ValueFactory.

    public IntegerValue createIntegerValue(int min, int max)
    {
        return min == max ?
            new ParticularIntegerValue(min) :
            new RangeIntegerValue(min, max);
    }
}
