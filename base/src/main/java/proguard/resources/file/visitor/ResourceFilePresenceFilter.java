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

import proguard.resources.file.FilePool;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.ResourceFilePool;
import proguard.resources.kotlinmodule.KotlinModule;

/**
 * This {@link ResourceFileVisitor} delegates its visits to one of two
 * {@link ResourceFileVisitor} instances, depending on whether the name of
 * the visited resource file is present in a given {@link FilePool} or not.
 *
 * @author Thomas Neidhart
 */
public class ResourceFilePresenceFilter
    implements   ResourceFileVisitor
{
    private final FilePool            filePool;
    private final ResourceFileVisitor presentResourceFileVisitor;
    private final ResourceFileVisitor missingResourceFileVisitor;


    /**
     * Creates a new ResourceFilePresenceFilter.
     * @param filePool           the <code>ResourceFilePool</code> in which the
     *                                   presence will be tested.
     * @param presentResourceFileVisitor the <code>ResourceFileVisitor</code> to which visits
     *                                   of present resource files will be delegated.
     * @param missingResourceFileVisitor the <code>ResourceFileVisitor</code> to which visits
     *                                   of missing resource files will be delegated.
     */
    public ResourceFilePresenceFilter(FilePool            filePool,
                                      ResourceFileVisitor presentResourceFileVisitor,
                                      ResourceFileVisitor missingResourceFileVisitor)
    {
        this.filePool                   = filePool;
        this.presentResourceFileVisitor = presentResourceFileVisitor;
        this.missingResourceFileVisitor = missingResourceFileVisitor;
    }

    /**
     * Creates a new ResourceFilePresenceFilter.
     * @param resourceFilePool           the <code>ResourceFilePool</code> in which the
     *                                   presence will be tested.
     * @param presentResourceFileVisitor the <code>ResourceFileVisitor</code> to which visits
     *                                   of present resource files will be delegated.
     * @param missingResourceFileVisitor the <code>ResourceFileVisitor</code> to which visits
     *                                   of missing resource files will be delegated.
     */
    @Deprecated
    public ResourceFilePresenceFilter(ResourceFilePool    resourceFilePool,
                                      ResourceFileVisitor presentResourceFileVisitor,
                                      ResourceFileVisitor missingResourceFileVisitor)
    {
        this.filePool                   = resourceFilePool;
        this.presentResourceFileVisitor = presentResourceFileVisitor;
        this.missingResourceFileVisitor = missingResourceFileVisitor;
    }

    // Implementations for ResourceFileVisitor.

    @Override
    public void visitResourceFile(ResourceFile resourceFile)
    {
        ResourceFileVisitor resourceFileVisitor = resourceFileVisitor(resourceFile);

        if (resourceFileVisitor != null)
        {
            resourceFileVisitor.visitResourceFile(resourceFile);
        }
    }


    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        ResourceFileVisitor resourceFileVisitor = resourceFileVisitor(kotlinModule);

        if (resourceFileVisitor != null)
        {
            resourceFileVisitor.visitResourceFile(kotlinModule);
        }
    }

    // Small utility methods.

    /**
     * Returns the appropriate <code>ResourceFileVisitor</code>.
     */
    protected ResourceFileVisitor resourceFileVisitor(ResourceFile resourceFile)
    {
        return filePool.getResourceFile(resourceFile.getFileName()) != null ?
               presentResourceFileVisitor :
               missingResourceFileVisitor;
    }
}
