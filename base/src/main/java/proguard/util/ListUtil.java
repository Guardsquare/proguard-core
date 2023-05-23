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
package proguard.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * This class provides some utility methods for working with
 * <code>java.util.List</code> instances.
 *
 * @author Eric Lafortune
 */
public class ListUtil
{
    /**
     * Returns a List instance that contains the elements of the two given List
     * instances. The instances may be null. If both instances are null, the
     * result is null too.
     */
    public static <T> List<T> concatenate(List<T> list1, List<T> list2)
    {
        if (list1 != null)
        {
            if (list2 != null)
            {
                // Combine the lists into a new list.
                List<T> list = new ArrayList<>(list1);
                list.addAll(list2);

                return list;
            }
            else
            {
                // Ignore the second (null) list.
                return list1;
            }
        }
        else
        {
            // Ignore the first (null) list.
            return list2;
        }
    }


    /**
     * Returns the subset of the given list, containing all strings that
     * match the given filter.
     */
    public static List<String> filter(Collection<String> list,
                                      StringMatcher      filter)
    {
        List<String> filteredList = new ArrayList<>();
        for (String string : list)
        {
            if (filter.matches(string))
            {
                filteredList.add(string);
            }
        }
        return filteredList;
    }


    /**
     * Creates a comma-separated String from the given List of String objects.
     */
    public static String commaSeparatedString(List<String> list, boolean quoteStrings)
    {
        if (list == null)
        {
            return null;
        }

        StringBuilder buffer = new StringBuilder();

        for (int index = 0; index < list.size(); index++)
        {
            if (index > 0)
            {
                buffer.append(',');
            }

            String string = list.get(index);

            if (quoteStrings)
            {
                string = quotedString(string);
            }

            buffer.append(string);
        }

        return buffer.toString();
    }


    /**
     * Creates a List of String objects from the given comma-separated String.
     */
    public static List<String> commaSeparatedList(String string)
    {
        if (string == null)
        {
            return null;
        }

        List<String> list = new ArrayList<>();
        int index = 0;
        while ((index = skipWhitespace(string, index)) < string.length())
        {
            int nextIndex;

            // Do we have an opening quote?
            if (string.charAt(index) == '\'')
            {
                // Parse a quoted string.
                nextIndex = string.indexOf('\'', index + 1);
                if (nextIndex < 0)
                {
                    nextIndex = string.length();
                }

                list.add(string.substring(index + 1, nextIndex));
            }
            else
            {
                // Parse a non-quoted string.
                nextIndex = string.indexOf(',', index);
                if (nextIndex < 0)
                {
                    nextIndex = string.length();
                }

                String substring = string.substring(index, nextIndex).trim();
                if (substring.length() > 0)
                {
                    list.add(substring);
                }
            }

            index = nextIndex + 1;
        }

        return list;
    }


    /**
     * Converts a List of Integers to an int array.
     *
     * @param integerList the List of Integers to convert.
     * @return the corresponding int array.
     */
    public static int[] toIntArray(List<Integer> integerList)
    {
        int[] intArray = new int[integerList.size()];
        for (int i = 0; i < intArray.length; i++)
        {
            intArray[i] = integerList.get(i);
        }
        return intArray;
    }


    /**
     * Skips any whitespace characters.
     */
    private static int skipWhitespace(String string, int index)
    {
        while (index < string.length() &&
               Character.isWhitespace(string.charAt(index)))
        {
            index++;
        }
        return index;
    }


    /**
     * Returns a quoted version of the given string, if necessary.
     */
    private static String quotedString(String string)
    {
        return string.length()     == 0 ||
               string.indexOf(' ') >= 0 ||
               string.indexOf('@') >= 0 ||
               string.indexOf('{') >= 0 ||
               string.indexOf('}') >= 0 ||
               string.indexOf('(') >= 0 ||
               string.indexOf(')') >= 0 ||
               string.indexOf(':') >= 0 ||
               string.indexOf(';') >= 0 ||
               string.indexOf(',') >= 0  ? ("'" + string + "'") :
                                           (      string      );
    }


    public static void main(String[] args)
    {
        if (args.length == 1)
        {
            System.out.println("Input string: ["+args[0]+"]");

            List<String> list = commaSeparatedList(args[0]);

            System.out.println("Resulting list:");
            for (String s : list)
            {
                System.out.println("[" + s + "]");
            }
        }
        else
        {
            List<String> list = Arrays.asList(args);

            System.out.println("Input list:");
            for (String s : list)
            {
                System.out.println("[" + s + "]");
            }

            String string = commaSeparatedString(list, true);

            System.out.println("Resulting string: ["+string+"]");
        }
    }
}
