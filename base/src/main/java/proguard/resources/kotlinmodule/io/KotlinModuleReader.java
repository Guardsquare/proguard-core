/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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

import kotlinx.metadata.jvm.*;
import proguard.classfile.*;
import proguard.resources.file.visitor.*;
import proguard.resources.kotlinmodule.*;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * Read an input stream into a KotlinModule object.
 *
 * @author James Hamilton
 */
public class KotlinModuleReader
implements   ResourceFileVisitor
{
    private final BiConsumer<KotlinModule, String> errorHandler;
    private final InputStream                      inputStream;

    public KotlinModuleReader(InputStream inputStream)
    {
        this(null, inputStream);
    }

    public KotlinModuleReader(BiConsumer<KotlinModule, String> errorHandler, InputStream inputStream) {
        this.errorHandler = errorHandler;
        this.inputStream  = inputStream;
    }

    @Override
    public void visitKotlinModule(KotlinModule kotlinModule)
    {
        try
        {
            // The Kotlin metadata API requires a byte array,
            // so copy the inputstream to a byte output stream
            // which we can then easily convert to a byte array.
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = inputStream.read(buffer);
                if (count < 0) break;
                byteStream.write(buffer, 0, count);
            }

            byte[]               bytes                = byteStream.toByteArray();
            KotlinModuleMetadata kotlinModuleMetadata = KotlinModuleMetadata.read(bytes);
            KmModule kmModule                         = requireNonNull(kotlinModuleMetadata).toKmModule();

            // Now we have the KmModule object we can
            // use a visitor to initialize our own KotlinModule object.
            kmModule.accept(new KmModuleVisitor()
            {
                @SuppressWarnings("NullableProblems")
                public void visitPackageParts(String fqName, List<String> fileFacades, Map<String, String> multiFileClassParts)
                {
                    kotlinModule.modulePackages.add(
                        new KotlinModulePackage(
                            fqName.replace(JavaTypeConstants.PACKAGE_SEPARATOR, TypeConstants.PACKAGE_SEPARATOR),
                            fileFacades,
                            multiFileClassParts));
                }
            });
        }
        catch (NullPointerException | IOException e)
        {
            if (this.errorHandler != null)
            {
                this.errorHandler.accept(kotlinModule, e.getMessage());
            }
            else
            {
                throw new RuntimeException("Error while reading Kotlin module file", e);
            }
        }
    }
}
