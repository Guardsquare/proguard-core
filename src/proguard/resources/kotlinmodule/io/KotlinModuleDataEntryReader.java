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
