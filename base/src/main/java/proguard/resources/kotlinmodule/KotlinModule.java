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

package proguard.resources.kotlinmodule;

import proguard.classfile.kotlin.KotlinMetadataVersion;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.visitor.ResourceFileVisitor;
import proguard.resources.kotlinmodule.visitor.*;
import proguard.util.Processable;

import java.util.*;

import static proguard.classfile.kotlin.KotlinConstants.*;

/**
 * Represents a Kotlin module file - this file describes the contents of
 * a Kotlin module: which file facades and which multi-file part classes
 * make up the module.
 *
 * The name of the module is the filename, excluding the extension (.kotlin_module).
 *
 * @author James Hamilton
 */
public class KotlinModule
extends      ResourceFile
implements   Processable
{
    /**
     * The module name, as opposed to the fileName which includes the module name.
     *
     * fileName will be like "META-INF/my_module_name.kotlin_module".
     *
     * The fileName is fixed to match after obfuscation by KotlinModuleReferenceFixer.
     */
    public String name;
    public KotlinMetadataVersion version;

    public final List<KotlinModulePackage> modulePackages = new ArrayList<>();

    public KotlinModule(String fileName, long fileSize)
    {
        super(fileName, fileSize);
        this.name = fileNameToModuleName(fileName);
    }

    public void modulePackagesAccept(KotlinModulePackageVisitor kotlinModulePackageVisitor)
    {
        modulePackages.forEach(modulePart -> kotlinModulePackageVisitor.visitKotlinModulePackage(this, modulePart));
    }

    // Implementations for ResourceFile.

    @Override
    public void accept(ResourceFileVisitor resourceFileVisitor)
    {
        resourceFileVisitor.visitKotlinModule(this);
    }

    public void accept(KotlinModuleVisitor kotlinModuleVisitor)
    {
        kotlinModuleVisitor.visitKotlinModule(this);
    }

    // Small helper methods.

    private static String fileNameToModuleName(String fileName)
    {
        return fileName.substring(fileName.indexOf('/') + 1, fileName.lastIndexOf(MODULE.FILE_EXTENSION));
    }
}
