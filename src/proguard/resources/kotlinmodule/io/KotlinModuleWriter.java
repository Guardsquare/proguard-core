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

package proguard.resources.kotlinmodule.io;

import kotlinx.metadata.internal.metadata.jvm.deserialization.JvmMetadataVersion;
import kotlinx.metadata.jvm.*;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.classfile.util.ClassUtil;
import proguard.resources.file.visitor.*;

import java.io.*;


/**
 * @author James Hamilton
 */
public class KotlinModuleWriter
implements   ResourceFileVisitor
{
    private final OutputStream outputStream;

    public KotlinModuleWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        try
        {
            KotlinModuleMetadata.Writer writer   = new KotlinModuleMetadata.Writer();
            KmModule                    kmModule = new KmModule();

            kotlinModule.modulePackagesAccept(
                (module, modulePackage) ->
                    kmModule.visitPackageParts(
                        ClassUtil.externalClassName(modulePackage.fqName),
                        modulePackage.fileFacadeNames,
                        modulePackage.multiFileClassParts
                    )
            );

            kmModule.visitEnd();

            kmModule.accept(writer);

            byte[] transformedBytes = writer.write(JvmMetadataVersion.INSTANCE.toArray()).getBytes();
            outputStream.write(transformedBytes);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while writing module file", e);
        }
    }
}
