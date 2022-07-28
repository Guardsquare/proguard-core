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
package proguard.util;

/**
 * This {@link StringFunction} removes a given suffix from each transformed String,
 * if present.
 *
 * @author Johan Leys
 * @author Eric Lafortune
 */
public class SuffixRemovingStringFunction
implements   StringFunction
{
    private final String suffix;


    /**
     * Creates a new SuffixRemovingStringFunction.
     *
     * @param suffix the suffix to remove from each string.
     */
    public SuffixRemovingStringFunction(String suffix)
    {
        this.suffix = suffix;
    }


    // Implementations for StringFunction.

    public String transform(String string)
    {
        return string.endsWith(suffix) ?
            string.substring(0, string.length() - suffix.length()) :
            string;
    }
}
