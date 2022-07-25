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

package proguard.util;

import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.Stack;
import proguard.evaluation.value.Value;

/**
 * Helper functions to access PartialEvaluator results mode conveniently.
 *
 * @author Dennis Titze
 */
public class PartialEvaluatorUtils
{

    /**
     * Returns the value from the stack, counting from the top of the stack.
     * If the stack does not contain that many elements, returns null.
     */
    public static Value getStackValue(Stack stack, int indexFromTop)
    {
        if (stack != null && stack.size() > indexFromTop)
        {
            return stack.getTop(indexFromTop);
        }
        return null;
    }

    /**
     * Returns the value from the before stack at offset, counting from the top. If the stack does not
     * contain that many elements, returns null.
     */
    public static Value getStackBefore(PartialEvaluator partialEvaluator, int offset, int index)
    {
        if (partialEvaluator.getStackBefore(offset) != null
            && partialEvaluator.getStackBefore(offset).size() > index)
        {
            return partialEvaluator.getStackBefore(offset).getTop(index);
        }
        return null;
    }
}
