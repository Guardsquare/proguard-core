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

import proguard.exception.ErrorId;

/**
 * Exception thrown when the variable index is out of bound of the current Variable count.
 */
public class VariableIndexOutOfBoundException extends VariableEvaluationException
{
    /**
     * The bound that has been invalidated.
     */
    private final int bound;

    public VariableIndexOutOfBoundException(int index, int bound)
    {
        super("Variable index [%s] out of bounds. There are %s variables in this code attribute.",
                ErrorId.VARIABLE_INDEX_OUT_OF_BOUND,
                new String[] {Integer.toString(index), Integer.toString(bound)}, index, null);
        this.bound = bound;
    }

    public int getBound()
    {
        return bound;
    }
}
