/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */
package proguard.resources.kotlinmodule.io;

import proguard.resources.kotlinmodule.KotlinModule;
import proguard.io.*;
import proguard.resources.file.ResourceFilePool;

import java.io.*;

/**
 * Write a {@link KotlinModule} from a {@link ResourceFilePool} to the supplied {@link DataEntryWriter}.
 *
 * @author James Hamilton
 */
public class KotlinModuleDataEntryWriter
implements   DataEntryWriter
{
    private final ResourceFilePool resourceFilePool;
    private final DataEntryWriter  dataEntryWriter;


    public KotlinModuleDataEntryWriter(ResourceFilePool resourceFilePool, DataEntryWriter dataEntryWriter)
    {
        this.resourceFilePool = resourceFilePool;
        this.dataEntryWriter  = dataEntryWriter;
    }


    // Implementations for DataEntryWriter.

    @Override
    public boolean createDirectory(DataEntry dataEntry) throws IOException
    {
        return dataEntryWriter.createDirectory(dataEntry);
    }


    @Override
    public boolean sameOutputStream(DataEntry dataEntry1, DataEntry dataEntry2) throws IOException
    {
        return dataEntryWriter.sameOutputStream(dataEntry1, dataEntry2);
    }

    @Override
    public OutputStream createOutputStream(DataEntry dataEntry) throws IOException
    {
        KotlinModule km = (KotlinModule)resourceFilePool.getResourceFile(dataEntry.getName());

        if (km != null)
        {
            OutputStream outputStream = new BufferedOutputStream(dataEntryWriter.createOutputStream(dataEntry));
            km.accept(new KotlinModuleWriter(outputStream));
            outputStream.flush();
            outputStream.close();
        }

        return null;
    }

    @Override
    public void close() throws IOException
    {
        dataEntryWriter.close();
    }

    @Override
    public void println(PrintWriter pw, String prefix)
    {
        pw.println(prefix + "KotlinModuleDataEntryWriter");
        dataEntryWriter.println(pw, prefix);
    }
}