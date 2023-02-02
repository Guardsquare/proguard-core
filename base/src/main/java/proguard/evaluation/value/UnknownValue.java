/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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
 * Represents a value that is completely unknown.
 */
public class UnknownValue extends Value
{
    UnknownValue() { }

    @Override
    public Value generalize(Value other)
    {
        return this;
    }

    @Override
    public boolean isCategory2()
    {
        return false;
    }

    @Override
    public int computationalType()
    {
        return Value.TYPE_UNKNOWN;
    }

    @Override
    public String internalType()
    {
        return null;
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof UnknownValue;
    }

    @Override
    public String toString()
    {
        return "UNKNOWN";
    }
}
