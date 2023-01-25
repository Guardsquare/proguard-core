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

package proguard.resources.file;

import java.util.List;
import java.util.Set;
import proguard.resources.file.visitor.ResourceFileVisitor;
import proguard.util.FileNameParser;
import proguard.util.ListParser;
import proguard.util.StringMatcher;

/**
 * Interface with methods related to file pools.
 */
public interface FilePool
{
    /**
     * Returns a Set of all resource file names in this resource file pool.
     */
    Set<String> resourceFileNames();

    /**
     * Returns a ResourceFile from this pool, based on its name.
     * Returns <code>null</code> if the instance with the given name is not in the pool.
     */
    ResourceFile getResourceFile(String fileName);

    /**
     * Applies the given ResourceFileVisitor to all instances in this pool.
     */
    void resourceFilesAccept(ResourceFileVisitor resourceFileVisitor);


    /**
     * Applies the given ResourceFileVisitor to all resource files in this pool matching the given file name filter.
     */
    default void resourceFilesAccept(String fileNameFilter, ResourceFileVisitor resourceFileVisitor)
    {
        resourceFilesAccept(new ListParser(new FileNameParser()).parse(fileNameFilter),
                            resourceFileVisitor);
    }


    /**
     * Applies the given ResourceFileVisitor to all resource files in this pool matching the given file name filters.
     */
    default void resourceFilesAccept(List<String> fileNameFilter, ResourceFileVisitor resourceFileVisitor)
    {
        resourceFilesAccept(new ListParser(new FileNameParser()).parse(fileNameFilter),
                            resourceFileVisitor);
    }


    /**
     * Applies the given ResourceFileVisitor to all resource files in this pool matching the given file name filter.
     */
    void resourceFilesAccept(StringMatcher fileNameFilter, ResourceFileVisitor resourceFileVisitor);


    /**
     * Applies the given ResourceFileVisitor to the instance with the given name, if it is present in this pool.
     */
    default void resourceFileAccept(String fileName, ResourceFileVisitor resourceFileVisitor)
    {
        ResourceFile resourceFile = getResourceFile(fileName);
        if (resourceFile != null)
        {
            resourceFile.accept(resourceFileVisitor);
        }
    }
}
