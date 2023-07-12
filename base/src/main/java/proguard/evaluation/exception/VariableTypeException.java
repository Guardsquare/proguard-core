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

package proguard.evaluation.exception;

import proguard.evaluation.value.Value;

import static proguard.classfile.util.ClassUtil.externalType;

/**
 * Exception thrown when the type in a variable does not match the expected type.
 */
public class VariableTypeException extends VariableEvaluationException
{
    /**
     * The type that was expected but not given and caused this exception.
     */
    private final char expectedType;

    /**
     * The type that was found to be of incorrect type.
     */
    private final Value foundValue;

    public VariableTypeException(int index, Value foundValue, char expectedType, Throwable cause)
    {
        super("Value in slot %s of type %s expected, but found: %s ",
                new String[] {Integer.toString(index), externalType(Character.toString(expectedType)), foundValue.toString()}, index, cause);
        this.expectedType = expectedType;
        this.foundValue = foundValue;
    }

    public char getExpectedType()
    {
        return expectedType;
    }

    public Value getFoundValue()
    {
        return foundValue;
    }
}
