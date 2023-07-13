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
package proguard.evaluation.exception;

import proguard.evaluation.PartialEvaluator;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

/**
 * Represents an exception during partial evaluation when a single instruction would be visited more than {@link PartialEvaluator#stopAnalysisAfterNEvaluations(int)} times.
 * In this case, the analysis will forcibly stop by throwing this exception.
 *
 * @author Dennis Titze
 */
public class ExcessiveComplexityException
    extends ProguardCoreException
{
    public ExcessiveComplexityException(String message)
    {
        super(ErrorId.EXCESSIVE_COMPLEXITY, message);
    }
}

