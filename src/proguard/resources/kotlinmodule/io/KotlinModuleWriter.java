/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
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
extends      SimplifiedResourceFileVisitor
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
