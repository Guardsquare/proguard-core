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

import proguard.exception.ProguardCoreException;

/**
 * Partial evaluator exception regarding Variables.
 */
public abstract class VariableEvaluationException extends ProguardCoreException
{
    /**
     * The index of the variable this exception is about.
     */
    private final int index;

    public VariableEvaluationException(String message, int componentErrorId, String[] errorParameters, int index, Throwable cause)
    {
        super( componentErrorId, cause, message, errorParameters);
        this.index = index;
    }

    public int getIndex()
    {
        return index;
    }
}
