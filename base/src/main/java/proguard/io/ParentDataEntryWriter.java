/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
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

import java.io.*;

/**
 * This {@link DataEntryWriter} lets another {@link DataEntryWriter} write the parent data
 * entries.
 *
 * @author Eric Lafortune
 */
public class ParentDataEntryWriter implements DataEntryWriter
{
    private DataEntryWriter dataEntryWriter;


    /**
     * Creates a new ParentDataEntryWriter.
     * @param dataEntryWriter the DataEntryWriter to which the writing will be
     *                        delegated, passing the data entries' parents.
     */
    public ParentDataEntryWriter(DataEntryWriter dataEntryWriter)
    {
        this.dataEntryWriter = dataEntryWriter;
    }


    // Implementations for DataEntryWriter.

    @Override
    public boolean createDirectory(DataEntry dataEntry) throws IOException
    {
        return dataEntryWriter.createDirectory(dataEntry.getParent());
    }


    @Override
    public boolean sameOutputStream(DataEntry dataEntry1,
                                    DataEntry dataEntry2)
    throws IOException
    {
        return dataEntryWriter.sameOutputStream(dataEntry1.getParent(),
                                                dataEntry2.getParent());
    }


    @Override
    public OutputStream createOutputStream(DataEntry dataEntry) throws IOException
    {
        return dataEntryWriter.createOutputStream(dataEntry.getParent());
    }


    @Override
    public void close() throws IOException
    {
        if (dataEntryWriter != null)
        {
            dataEntryWriter.close();
            dataEntryWriter = null;
        }
    }


    @Override
    public void println(PrintWriter pw, String prefix)
    {
        pw.println(prefix + "ParentDataEntryWriter");
        dataEntryWriter.println(pw, prefix + "  ");
    }
}
