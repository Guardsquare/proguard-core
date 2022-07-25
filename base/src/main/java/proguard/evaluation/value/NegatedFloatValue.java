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
package proguard.evaluation.value;

/**
 * This {@link FloatValue} represents a float value that is negated.
 *
 * @author Eric Lafortune
 */
public final class NegatedFloatValue extends SpecificFloatValue
{
    private final FloatValue floatValue;


    /**
     * Creates a new negated float value of the given float value.
     */
    public NegatedFloatValue(FloatValue floatValue)
    {
        this.floatValue = floatValue;
    }


    // Implementations of unary methods of FloatValue.

    public FloatValue negate()
    {
        return floatValue;
    }


    // Implementations for Object.

    public boolean equals(Object object)
    {
        return this == object ||
               super.equals(object) &&
               this.floatValue.equals(((NegatedFloatValue)object).floatValue);
    }


    public int hashCode()
    {
        return super.hashCode() ^
               floatValue.hashCode();
    }


    public String toString()
    {
        return "-"+floatValue;
    }
}
