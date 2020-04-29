/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule.io;

import kotlinx.metadata.jvm.*;
import proguard.classfile.*;
import proguard.resources.file.visitor.*;
import proguard.resources.kotlinmodule.*;

import java.io.*;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Read an input stream into a KotlinModule object.
 *
 * @author James Hamilton
 */
public class KotlinModuleReader
implements   ResourceFileVisitor
{
    private final InputStream inputStream;

    public KotlinModuleReader(InputStream inputStream) {
        this.inputStream = inputStream;
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
            throw new RuntimeException("Error while reading Kotlin module file", e);
        }
    }
}
