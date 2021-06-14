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

package proguard.classfile.util.kotlin;

/**
 * Kotlin String utility methods.
 *
 * @author James Hamilton
 */
public class KotlinStringUtil
{
    private KotlinStringUtil() {}

    public static String capitializeFirstCharacterAsciiOnly(String s)
    {
        if (s.length() == 0)
        {
            return s;
        }

        char firstChar = s.charAt(0);

        if (firstChar >= 'a' && firstChar <= 'z')
        {
            return Character.toUpperCase(firstChar) + s.substring(1);
        }
        else
        {
            return s;
        }
    }

    public static String decapitializeFirstCharacterAsciiOnly(String s)
    {
        if (s.length() == 0)
        {
            return s;
        }

        char firstChar = s.charAt(0);

        if (firstChar >= 'A' && firstChar <= 'Z')
        {
            return Character.toLowerCase(firstChar) + s.substring(1);
        }
        else
        {
            return s;
        }
    }

    /**
     *  Decaptialize according to the Kotlin compiler, basically first words
     *  are decapitialized.
     *
     *  See capitializeDecapitialze.kt
     *
     * "FooBar"  -> "fooBar"
     * "FOOBar"  -> "fooBar"
     * "FOO"     -> "foo"
     * "FOO_BAR" -> "foo_BAR"
     */
    public static String decapitializeForKotlinCompiler(String s)
    {
        if (s.length() == 0 || !isUpperCaseAsciiCharAt(s, 0))
        {
            return s;
        }

        if (s.length() == 1 || !isUpperCaseAsciiCharAt(s, 1))
        {
            return decapitializeFirstCharacterAsciiOnly(s);
        }

        int secondWordStart = -1;
        for (int i = 0;  i < s.length(); i++)
        {
            if (!isUpperCaseAsciiCharAt(s, i))
            {
                secondWordStart = i;
                break;
            }
        }

        return secondWordStart == -1    ?
                toLowerCaseAsciiOnly(s) :
                toLowerCaseAsciiOnly(s.substring(0, secondWordStart)) + s.substring(secondWordStart);
    }

    private static boolean isUpperCaseAsciiCharAt(String s, int index)
    {
        char charAtIndex = s.charAt(index);
        return charAtIndex >= 'A' && charAtIndex <= 'Z';
    }

    private static String toLowerCaseAsciiOnly(String s)
    {
        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray())
        {
            if (c >= 'A' && c <= 'Z')
            {
                sb.append(Character.toLowerCase(c));
            }
            else
            {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
