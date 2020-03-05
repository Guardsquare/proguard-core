/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.resources.kotlinmodule.io;

import proguard.resources.kotlinmodule.KotlinModule;
import proguard.io.*;
import proguard.resources.file.visitor.ResourceFileVisitor;

import java.io.*;

/**
 * Read a Kotlin module file and apply the given {@link ResourceFileVisitor}.
 *
 * @author James Hamilton
 */
public class KotlinModuleDataEntryReader
implements   DataEntryReader
{
    private final ResourceFileVisitor resourceFileVisitor;

    public KotlinModuleDataEntryReader(ResourceFileVisitor resourceFileVisitor)
    {
        this.resourceFileVisitor = resourceFileVisitor;
    }

    @Override
    public void read(DataEntry dataEntry) throws IOException
    {
        try
        {
            InputStream inputStream   = dataEntry.getInputStream();
            KotlinModule kotlinModule = new KotlinModule(dataEntry.getName(), dataEntry.getSize());
            kotlinModule.accept(new KotlinModuleReader(inputStream));
            dataEntry.closeInputStream();
            kotlinModule.accept(resourceFileVisitor);
        }
        catch (Exception ex)
        {
            throw new IOException("Can't read Kotlin module file [" + dataEntry.getName() + "] (" + ex.getMessage() + ")", ex);
        }
    }
}