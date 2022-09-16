/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2022 Guardsquare NV
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
package proguard.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * This <code>DataEntry</code> represents a named output entry with a parent.
 *
 * @author Eric Lafortune
 */
public class NamedDataEntry implements DataEntry
{
    private final DataEntry parent;
    private final String    name;
    private final long      size;
    private final boolean   isDirectory;


    public NamedDataEntry(DataEntry parent,
                          String    name,
                          long      size,
                          boolean   isDirectory)
    {
        this.parent      = parent;
        this.name        = name;
        this.size        = size;
        this.isDirectory = isDirectory;
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
        return size;
    }


    @Override
    public boolean isDirectory()
    {
        return isDirectory;
    }


    @Override
    public InputStream getInputStream() throws IOException
    {
        throw new UnsupportedOperationException("Can't retrieve input stream for output entry ["+name+"]");
    }


    @Override
    public void closeInputStream() throws IOException
    {
    }


    @Override
    public DataEntry getParent()
    {
        return parent;
    }


    // Implementations for Object.

    @Override
    public String toString()
    {
        return String.valueOf(parent) + ':' + name;
    }
}
