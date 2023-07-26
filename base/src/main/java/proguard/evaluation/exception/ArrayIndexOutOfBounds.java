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

import proguard.evaluation.PartialEvaluator;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

/**
 * Exception thrown when the index to access an array is known to be out of bound for the array.
 *
 * @see PartialEvaluator
 */
public class ArrayIndexOutOfBounds extends ProguardCoreException
{
    private final int index;

    private final int bound;

    public ArrayIndexOutOfBounds(int index, int bound)
    {
        super(ErrorId.ARRAY_INDEX_OUT_OF_BOUND,
                  "Index [%s] out of bounds for array of length %s",
                  Integer.toString(index), Integer.toString(bound));
        this.index = index;
        this.bound = bound;
    }

    public int getIndex()
    {
        return index;
    }

    public int getBound()
    {
        return bound;
    }
}
