/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

import kotlinx.metadata.jvm.KmModule;
import kotlinx.metadata.jvm.KmPackageParts;
import kotlinx.metadata.jvm.KotlinModuleMetadata;
import proguard.resources.file.visitor.ResourceFileVisitor;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.classfile.util.ClassUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;

import static kotlinx.metadata.jvm.KotlinClassMetadata.COMPATIBLE_METADATA_VERSION;


/**
 * @author James Hamilton
 */
public class KotlinModuleWriter
implements   ResourceFileVisitor
{
    private final OutputStream                     outputStream;
    private final BiConsumer<KotlinModule, String> errorHandler;

    public KotlinModuleWriter(OutputStream outputStream)
    {
        this(null, outputStream);
    }

    public KotlinModuleWriter(BiConsumer<KotlinModule, String> errorHandler, OutputStream outputStream)
    {
        this.errorHandler = errorHandler;
        this.outputStream = outputStream;
    }

    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        try
        {
            KmModule kmModule = new KmModule();

            kotlinModule.modulePackagesAccept(
                (module, modulePackage) ->
                    kmModule.getPackageParts().put(
                        ClassUtil.externalClassName(modulePackage.fqName),
                        new KmPackageParts(modulePackage.fileFacadeNames, modulePackage.multiFileClassParts)
                    )
            );

            // TODO: Support module optional annotations in our model.
            // kmModule.getOptionalAnnotationClasses();

            byte[] transformedBytes = KotlinModuleMetadata.Companion.write(kmModule,
                kotlinModule.version.canBeWritten() ? kotlinModule.version.toArray() : COMPATIBLE_METADATA_VERSION).getBytes();
            outputStream.write(transformedBytes);
        }
        catch (IOException e)
        {
            if (this.errorHandler != null)
            {
                this.errorHandler.accept(kotlinModule, e.getMessage());
            }
            else
            {
                throw new RuntimeException("Error while writing module file", e);
            }
        }
    }
}
