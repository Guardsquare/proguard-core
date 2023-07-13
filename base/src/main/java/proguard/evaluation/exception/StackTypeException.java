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

package proguard.evaluation.exception;

import proguard.evaluation.value.Value;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

import static proguard.classfile.util.ClassUtil.externalType;

/**
 * Exception thrown when a type on the stack does not match the expected type.
 */
public class StackTypeException extends ProguardCoreException
{
    /**
     * The type that was expected but not given and caused this exception.
     */
    protected final char expectedType;

    /**
     * The value that was found to be of incorrect type.
     */
    protected final Value foundValue;

    public StackTypeException(Value foundValue, char expectedType, Throwable cause)
    {
        super(ErrorId.STACK_TYPE, cause, "Stack value of type %s expected, but found: %s.",
                externalType(Character.toString(expectedType)), foundValue.toString());
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