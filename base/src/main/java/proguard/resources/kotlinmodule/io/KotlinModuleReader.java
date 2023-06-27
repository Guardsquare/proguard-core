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
import kotlinx.metadata.jvm.KotlinModuleMetadata;
import proguard.classfile.JavaTypeConstants;
import proguard.classfile.TypeConstants;
import proguard.classfile.kotlin.KotlinMetadataVersion;
import proguard.resources.file.visitor.ResourceFileVisitor;
import proguard.resources.kotlinmodule.KotlinModule;
import proguard.resources.kotlinmodule.KotlinModulePackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static proguard.classfile.kotlin.KotlinMetadataVersion.UNKNOWN_VERSION;

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

            kotlinModule.version = getKotlinModuleMetadataVersion(bytes);

            // Now we have the KmModule object we can
            // use a visitor to initialize our own KotlinModule object.
            kotlinModule.modulePackages.addAll(
                kmModule
                    .getPackageParts()
                    .entrySet()
                    .stream()
                    .map(entry -> new KotlinModulePackage(
                            entry.getKey().replace(JavaTypeConstants.PACKAGE_SEPARATOR, TypeConstants.PACKAGE_SEPARATOR),
                            entry.getValue().getFileFacades(),
                            entry.getValue().getMultiFileClassParts()
                    ))
                   .collect(Collectors.toList())
            );

            // TODO: Support module optional annotations in our model.
            // kmModule.getOptionalAnnotationClasses();
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


    /**
     * Collects the KotlinMetadataVersion of the module.
     * @param module a byte[] of the KotlinModule.
     * @return The KotlinMetadataVersion of the module.
     */
    private KotlinMetadataVersion getKotlinModuleMetadataVersion(byte[] module)
    {
        try
        {
            // The version number of a module can be found from the first bytes of the module.
            // https://github.com/JetBrains/kotlin/blob/master/core/metadata.jvm/src/org/jetbrains/kotlin/metadata/jvm/deserialization/ModuleMapping.kt#L40
            // module: [ size, int[size] | ... ]
            // module: [ size, KmVersion | ... ]
            DataInputStream bytes = new DataInputStream(new ByteArrayInputStream(module));
            int size = bytes.readInt();
            if (size < 0 || size > 1024)
            {
                return UNKNOWN_VERSION;
            }

            int[] version = new int[size];
            for (int i=0; i<size; i++)
            {
                version[i] = bytes.readInt();
            }
            return new KotlinMetadataVersion(version);
        }
        catch ( IOException e)
        {
            return UNKNOWN_VERSION;
        }
    }
}
