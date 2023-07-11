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

import proguard.evaluation.PartialEvaluator;
import proguard.exception.ProguardCoreException;

import java.util.Collections;

/**
 * Represents an exception when the `PartialEvaluator` encounters a semantically incorrect java bytecode instruction.
 *
 * @see PartialEvaluator
 */
public class PartialEvaluatorException extends ProguardCoreException
{
    public PartialEvaluatorException(String genericMessage)
    {
        super(genericMessage, 4, Collections.emptyList());
    }

    public PartialEvaluatorException(String genericMessage, Throwable cause)
    {
        super(genericMessage, 4, Collections.emptyList(), cause);
    }
}
