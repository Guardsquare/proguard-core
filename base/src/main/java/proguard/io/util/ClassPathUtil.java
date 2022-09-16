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
package proguard.io.util;

import proguard.io.ClassPath;
import proguard.io.ClassPathEntry;
import proguard.util.FileNameParser;
import proguard.util.ListParser;
import proguard.util.StringMatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    /**
     * Loop through all the input zip entries in {@link ClassPath} to determine the compression methods.
     * For dex files, if one entry is uncompressed, a regex is added to match all dex files.
     * That allows to keep a consistent compression method regardless of the number of dex files
     * after processing (edge case if we add classes and a new dex file is needed).
     *
     * @param classPath the entry to scan.
     */
    public static StringMatcher determineCompressionMethod(ClassPath classPath)
    {
        List<String> dontCompress = new ArrayList<>();
        for (int index = 0; index < classPath.size(); index++)
        {
            ClassPathEntry entry = classPath.get(index);
            if (!entry.isOutput())
            {
                dontCompress.addAll(determineCompressionMethod(entry));
            }
        }
        return dontCompress.isEmpty() ? null : new ListParser(new FileNameParser()).parse(dontCompress);
    }

    private static List<String> determineCompressionMethod(ClassPathEntry entry) {
        File file = entry.getFile();
        if (file == null || file.isDirectory())
        {
            return new ArrayList<>();
        }

        String regexDexClasses = "classes*.dex";

        try (ZipFile zip = new ZipFile(file))
        {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            Set<String> storedEntries = new TreeSet<>();

            while (entries.hasMoreElements())
            {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.getMethod() == ZipEntry.DEFLATED)
                {
                    continue;
                }

                String name = zipEntry.getName();

                // Special case for classes.dex: If we end up creating another dex file, we want all dex files compression to be consistent.
                if (name.matches("classes\\d*.dex"))
                {
                    storedEntries.add(regexDexClasses);
                }
                else
                {
                    storedEntries.add(name);
                }
            }

            return new ArrayList<>(storedEntries);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not determine compression method for " + entry.getName(), e);
        }
    }
}
