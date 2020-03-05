/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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
