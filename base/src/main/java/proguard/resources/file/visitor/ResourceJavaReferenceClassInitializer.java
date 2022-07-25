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

import proguard.classfile.*;
import proguard.classfile.util.ClassUtil;
import proguard.resources.file.*;
import proguard.resources.kotlinmodule.KotlinModule;

import java.util.Set;

/**
 * This {@link ResourceFileVisitor} initializes the class references from
 * non-binary resources files with the corresponding classes from the program
 * class pool.
 *
 * @author Lars Vandenbergh
 */
public class ResourceJavaReferenceClassInitializer
implements   ResourceFileVisitor
{
    private final ClassPool programClassPool;


    public ResourceJavaReferenceClassInitializer(ClassPool programClassPool)
    {
        this.programClassPool = programClassPool;
    }


    // Implementations for ResourceFileVisitor.

    @Override
    public void visitResourceFile(ResourceFile resourceFile)
    {
        Set<ResourceJavaReference> references = resourceFile.references;

        if (references != null)
        {
            for (ResourceJavaReference reference : references)
            {
                String externalClassName = reference.externalClassName;
                String internalClassName = ClassUtil.internalClassName(externalClassName);

                // Find the class corresponding to the fully qualified class name.
                reference.referencedClass = programClassPool.getClass(internalClassName);
            }
        }
    }


    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {

    }
}
