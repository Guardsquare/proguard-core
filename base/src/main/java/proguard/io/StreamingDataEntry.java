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
package proguard.io;

import java.io.IOException;
import java.io.InputStream;

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
