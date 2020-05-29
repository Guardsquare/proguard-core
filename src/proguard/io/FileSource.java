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
 * This DataEntrySource wraps a single given file or {@link DataEntry}.
 *
 * @author Eric Lafortune
 */
public class FileSource implements DataEntrySource
{
    private final FileDataEntry fileDataEntry;


    /**
     * Creates a new FileSource.
     * @param file the the absolute or relative location of the file.
     */
    public FileSource(File file)
    {
        this(null, file);
    }


    /**
     * Creates a new FileSource.
     * @param directory the base directory for the file.
     * @param file      the the absolute or relative location of the file.
     */
    public FileSource(File directory,
                      File file)
    {
        this(new FileDataEntry(directory, file));
    }


    /**
     * Creates a new FileSource.
     * @param fileDataEntry the file data entry.
     */
    public FileSource(FileDataEntry fileDataEntry)
    {
        this.fileDataEntry = fileDataEntry;
    }


    // Implementations for DataEntrySource.

    @Override
    public void pumpDataEntries(DataEntryReader dataEntryReader)
    throws IOException
    {
        // Pass the file data entry to the reader.
        dataEntryReader.read(fileDataEntry);
    }
}
