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
package proguard.resources.kotlinmodule.io;

import proguard.resources.file.visitor.ResourceFileVisitor;
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
    private final ResourceFilePool      resourceFilePool;
    private final DataEntryWriter       dataEntryWriter;


    public KotlinModuleDataEntryWriter(ResourceFilePool      resourceFilePool,
                                       DataEntryWriter       dataEntryWriter)
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
        KotlinModuleGetter kotlinModuleGetter = new KotlinModuleGetter();
        resourceFilePool.resourceFileAccept(dataEntry.getName(), kotlinModuleGetter);
        KotlinModule kotlinModule = kotlinModuleGetter.kotlinModule;

        if (kotlinModule != null)
        {
            OutputStream outputStream = new BufferedOutputStream(dataEntryWriter.createOutputStream(dataEntry));
            kotlinModule.accept(new KotlinModuleWriter(outputStream));
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

    private static class KotlinModuleGetter implements ResourceFileVisitor
    {
        KotlinModule kotlinModule;


        @Override
        public void visitKotlinModule(KotlinModule kotlinModule)
        {
            this.kotlinModule = kotlinModule;
        }
    }
}
