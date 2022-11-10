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
package proguard.resources.file.util;

import proguard.resources.file.FilePool;
import proguard.resources.file.ResourceFile;
import proguard.util.StringFunction;

/**
 * This {@link StringFunction} maps resource file names on their (obfuscated) resource file name, as present on the
 * {@link ResourceFile} object in the given resource file pool.
 *
 * @author Johan Leys
 */
public class FilePoolNameFunction
    implements StringFunction
{
    private final FilePool filePool;
    private final String   defaultResourceFileName;


    /**
     * Creates a new ResourceFileNameFunction based on the given resource file
     * pool.
     */
    public FilePoolNameFunction(FilePool filePool)
    {
        this(filePool, null);
    }


    /**
     * Creates a new ResourceFileNameFunction based on the given resource file
     * pool, with a default string for resource files that are not in the
     * resource file pool.
     */
    public FilePoolNameFunction(FilePool filePool,
                                String   defaultResourceFileName)
    {
        this.filePool                = filePool;
        this.defaultResourceFileName = defaultResourceFileName;
    }


    // Implementations for StringFunction.

    @Override
    public String transform(String string)
    {
        ResourceFile resourceFile = filePool.getResourceFile(string);

        return resourceFile != null ? resourceFile.getFileName() : defaultResourceFileName;
    }
}
