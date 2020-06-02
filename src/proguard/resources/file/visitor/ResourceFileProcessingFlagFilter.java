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
package proguard.resources.file.visitor;


import proguard.resources.file.ResourceFile;

/**
 * This ResourceFileVisitor delegates all its visits to a given delegate visitor, but only of the processing flags of
 * the visited resource file match the given processing flag requirements.
 *
 * @author Johan Leys
 */
public class ResourceFileProcessingFlagFilter
implements   ResourceFileVisitor
{
    private final int                 requiredSetProcessingFlags;
    private final int                 requiredUnsetProcessingFlags;
    private final ResourceFileVisitor acceptedVisitor;


    /**
     * Creates a new ResourceFileProcessingFlagFilter.
     *
     * @param requiredSetProcessingFlags   the  processing flags that should be set.
     * @param requiredUnsetProcessingFlags the class processing flags that should be unset.
     * @param acceptedVisitor              the <code>ResourceFileVisitor</code> to which visits will be delegated.
     */
    public ResourceFileProcessingFlagFilter(int                 requiredSetProcessingFlags,
                                            int                 requiredUnsetProcessingFlags,
                                            ResourceFileVisitor acceptedVisitor)
    {
        this.requiredSetProcessingFlags   = requiredSetProcessingFlags;
        this.requiredUnsetProcessingFlags = requiredUnsetProcessingFlags;
        this.acceptedVisitor              = acceptedVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyResourceFile(ResourceFile resourceFile)
    {
        if (accepted(resourceFile.getProcessingFlags()))
        {
            resourceFile.accept(acceptedVisitor);
        }
    }


    // Small utility methods.

    private boolean accepted(int accessFlags)
    {
        return (requiredSetProcessingFlags   & ~accessFlags) == 0 &&
               (requiredUnsetProcessingFlags &  accessFlags) == 0;
    }
}
