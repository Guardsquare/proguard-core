/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */
package proguard.io;

import java.io.IOException;
import java.io.InputStream;
import proguard.io.DataEntry;

/**
 * This <code>DataEntry</code> represents a stream of data.
 *
 * @author Thomas Neidhart
 */
public class StreamingDataEntry
    implements DataEntry
{
    private final String      name;
    private final InputStream inputStream;


    public StreamingDataEntry(String      name,
                              InputStream inputStream)
    {
        this.name        = name;
        this.inputStream = inputStream;
    }


    // Implementations for DataEntry.

    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public String getOriginalName()
    {
        return getName();
    }


    @Override
    public long getSize()
    {
        return -1;
    }


    @Override
    public boolean isDirectory()
    {
        return false;
    }


    @Override
    public InputStream getInputStream() throws IOException
    {
        return inputStream;
    }


    @Override
    public void closeInputStream() throws IOException
    {
        inputStream.close();
    }


    @Override
    public DataEntry getParent()
    {
        return null;
    }


    // Implementations for Object.

    @Override
    public String toString()
    {
        return getName();
    }
}
