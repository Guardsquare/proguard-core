/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
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
