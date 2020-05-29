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


import static proguard.classfile.util.kotlin.KotlinStringUtil.*;

/**
 * Kotlin utility methods to help with Kotlin naming conventions.
 *
 * See, for example, https://github.com/JetBrains/kotlin/blob/master/core/descriptors.jvm/src/org/jetbrains/kotlin/load/java/JvmAbi.java
 *
 * @author James Hamilton
 */
public class KotlinNameUtil
{
    public static final String GET_PREFIX = "get";
    public static final String SET_PREFIX = "set";
    public static final String IS_PREFIX  = "is";

    private KotlinNameUtil() {}


    public static String generateGetterName(String name)
    {
        return startsWithIsPrefix(name) ? name : prefixName(GET_PREFIX, name);
    }

    public static String generateSetterName(String name)
    {
        return prefixName(SET_PREFIX, startsWithIsPrefix(name) ? name.substring(IS_PREFIX.length()) : name);
    }

    public static boolean isGetterName(String name)
    {
        // Note: this does not call startsWithIsPrefix, so
        // e.g. isfalse is a getter name but does not start with "is" according
        // to startsWithIsPrefix - see org.jetbrains.kotlin.load.java.JvmAbi.

        return name.startsWith(GET_PREFIX) || name.startsWith(IS_PREFIX);
    }

    public static boolean isSetterName(String name)
    {
        return name.startsWith(SET_PREFIX);
    }

    /**
     * See Kotlin code:
     * https://github.com/JetBrains/kotlin/blob/a1569dfbf0b924eb717ce75069eb294fe3ea1c59/core/descriptors.jvm/src/org/jetbrains/kotlin/load/java/JvmAbi.java#L89
     *
     * @param name name
     * @return true if the name starts with the "is" prefix.
     */
    private static boolean startsWithIsPrefix(String name)
    {
        // It must start with "is" but cannot be just "is"

        if (!name.startsWith(IS_PREFIX) || name.equals(IS_PREFIX))
        {
            return false;
        }

        // Only if the first char after the "is" prefix is not lower-case ASCII.
        // e.g. isfalse      DOES NOT start with the "is" prefix.
        //      isπροβοσκιδα DOES     start with the "is" prefix (lower-case "π")

        char firstCharAfterIsPrefix = name.charAt(IS_PREFIX.length());
        return !('a' <= firstCharAfterIsPrefix && firstCharAfterIsPrefix <= 'z');
    }

    private static String prefixName(String prefix, String name)
    {
        if (name.length() == 0)
        {
            return name;
        }

        return prefix + capitializeFirstCharacterAsciiOnly(name);
    }


    /**
     * See propertiesConventionUtil.kt
     *
     * For example:
     *
     *  getMyProperty -> myProperty
     *  isProperty    -> property
     *  isfalse       -> isfalse
     *
     * @param name the property getter name e.g. getMyProperty
     * @param stripAfterDollar strip any extra info added by the compiler
     * @return the property name
     */
    public static String getterNameToPropertyName(String name, boolean stripAfterDollar)
    {
        if (stripAfterDollar && name.contains("$"))
        {
            name = name.substring(0, name.indexOf("$"));
        }

        if (isGetterName(name))
        {
            if (name.startsWith(GET_PREFIX))
            {
                return decapitializeForKotlinCompiler(name.substring(GET_PREFIX.length()));
            }
            else
            {
                // Don't strip "is" prefix.
                return name;
            }
        }
        else if (isSetterName(name))
        {
            return decapitializeForKotlinCompiler(name.substring(SET_PREFIX.length()));
        }
        else
        {
            return name;
        }
    }
}
