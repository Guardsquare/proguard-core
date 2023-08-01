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

import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

public class ArrayStoreTypeException extends ProguardCoreException
{
    private final TypedReferenceValue array;

    private final Value value;

    public ArrayStoreTypeException(TypedReferenceValue array, Value value)
    {
        super(ErrorId.ARRAY_STORE_TYPE_EXCEPTION,
                "Array of type [%s] can not store value [%s]",
                array.getType(), value.toString());
        this.array = array;
        this.value = value;
    }

    public TypedReferenceValue getArray()
    {
        return array;
    }

    public Value getValue()
    {
        return value;
    }
}
