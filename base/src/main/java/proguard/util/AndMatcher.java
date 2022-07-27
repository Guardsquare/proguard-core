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

/**
 * This {@link StringMatcher} tests whether strings match both given {@link StringMatcher}
 * instances.
 *
 * @author Eric Lafortune
 */
public class AndMatcher extends StringMatcher
{
    private final StringMatcher matcher1;
    private final StringMatcher matcher2;


    /**
     * Creates a new AndMatcher with the two given string matchers.
     */
    public AndMatcher(StringMatcher matcher1, StringMatcher matcher2)
    {
        this.matcher1 = matcher1;
        this.matcher2 = matcher2;
    }


    // Implementations for StringMatcher.

    @Override
    public String prefix()
    {
        String prefix1 = this.matcher1.prefix();
        String prefix2 = this.matcher2.prefix();

        if (prefix1 == null || prefix2 == null)
            return null;

        if (prefix1.length() < prefix2.length())
        {
            if (!prefix2.startsWith(prefix1))
                // can never match
                return null;
            return prefix1;
        }
        else if (!prefix1.startsWith(prefix2))
            // can never match
            return null;
        return prefix2;
    }

    @Override
    protected boolean matches(String string, int beginOffset, int endOffset)
    {
        return matcher1.matches(string, beginOffset, endOffset) &&
               matcher2.matches(string, beginOffset, endOffset);
    }
}
