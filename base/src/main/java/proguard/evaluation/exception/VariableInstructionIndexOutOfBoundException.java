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

/**
 *
 */
public class VariableInstructionIndexOutOfBoundException extends VariableInstructionEvaluationException
{
    // The bound that has been invalidated
    protected final int bound;

    public VariableInstructionIndexOutOfBoundException(int index, int bound) {
        super("Variable index ["+index+"] out of bounds. There are "+bound+" variables in this code attribute.", index);
        this.bound = bound;
    }
}
