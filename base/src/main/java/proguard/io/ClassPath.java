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
package proguard.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class represents a class path, as a list of ClassPathEntry objects.
 *
 * @author Eric Lafortune
 */
public class ClassPath
{
    private final List<ClassPathEntry> classPathEntries;

    public ClassPath(ClassPathEntry...entries)
    {
        classPathEntries = new ArrayList<>(Arrays.asList(entries));
    }

    /**
     * Returns whether the class path contains any output entries.
     */
    public boolean hasOutput()
    {
        return classPathEntries.stream().anyMatch(ClassPathEntry::isOutput);
    }


    /**
     * Returns the list of class path entries for this class path.
     */
    public List<ClassPathEntry> getClassPathEntries()
    {
        return classPathEntries;
    }


    // Delegates to List.

    public void clear()
    {
        classPathEntries.clear();
    }

    public void add(int index, ClassPathEntry classPathEntry)
    {
        classPathEntries.add(index, classPathEntry);
    }

    public boolean add(ClassPathEntry classPathEntry)
    {
        return classPathEntries.add(classPathEntry);
    }

    public boolean addAll(ClassPath classPath)
    {
        return classPathEntries.addAll(classPath.classPathEntries);
    }

    public ClassPathEntry get(int index)
    {
        return classPathEntries.get(index);
    }

    public ClassPathEntry remove(int index)
    {
        return classPathEntries.remove(index);
    }

    public boolean isEmpty()
    {
        return classPathEntries.isEmpty();
    }

    public int size()
    {
        return classPathEntries.size();
    }
}