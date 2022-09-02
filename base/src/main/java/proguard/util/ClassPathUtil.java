/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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

import proguard.io.ClassPath;
import proguard.io.ClassPathEntry;

public final class ClassPathUtil
{
    private ClassPathUtil() {}

    /**
     * Returns whether the class path contains any input app bundles.
     */
    public static boolean hasAabInput(ClassPath classPath)
    {
        return classPath != null &&
               classPath.getClassPathEntries().stream()
                   .filter(entry -> !entry.isOutput())
                   .anyMatch(ClassPathEntry::isAab);
    }

    /**
     * Returns whether the class path contains android target formats.
     */
    public static boolean isAndroid(ClassPath classPath)
    {
        return classPath != null &&
                classPath.getClassPathEntries().stream()
                        .anyMatch(entry -> entry.isDex() || entry.isApk() || entry.isAab() || entry.isAar());
    }

    /**
     * Returns whether the class path contains dalvik target formats.
     */
    public static boolean isDalvik(ClassPath classPath)
    {
        return classPath != null &&
                classPath.getClassPathEntries().stream()
                        .anyMatch(entry -> entry.isDex() || entry.isApk());
    }
}
