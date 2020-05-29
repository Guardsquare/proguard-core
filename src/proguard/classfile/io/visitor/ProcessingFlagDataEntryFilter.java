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
package proguard.classfile.io.visitor;

import proguard.io.*;
import proguard.resources.file.*;

/**
 * This DataEntryFilter filters data entries based on the processing flags of their corresponding resource file.
 *
 * @author Johan Leys
 */
public class ProcessingFlagDataEntryFilter implements DataEntryFilter
{
    private final ResourceFilePool resourceFilePool;
    private final int              requiredSetProcessingFlags;
    private final int              requiredUnsetProcessingFlags;


    public ProcessingFlagDataEntryFilter(ResourceFilePool resourceFilePool,
                                         int              requiredSetProcessingFlags,
                                         int              requiredUnsetProcessingFlags)
    {
        this.resourceFilePool             = resourceFilePool;
        this.requiredSetProcessingFlags   = requiredSetProcessingFlags;
        this.requiredUnsetProcessingFlags = requiredUnsetProcessingFlags;
    }


    // Implementations for DataEntryFilter.

    @Override
    public boolean accepts(DataEntry dataEntry)
    {
        ResourceFile resourceFile = resourceFilePool.getResourceFile(dataEntry.getName());

        if (resourceFile != null)
        {
            int processingFlags = resourceFile.getProcessingFlags();
            return (requiredSetProcessingFlags & ~processingFlags) == 0 &&
                   (requiredUnsetProcessingFlags & processingFlags) == 0;
        }

        return false;
    }
}
