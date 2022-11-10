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

package proguard.resources.file.visitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import proguard.io.DataEntry;
import proguard.io.DataEntryReader;
import proguard.io.DataEntryToken;
import proguard.io.DataEntryTokenType;
import proguard.io.DataEntryTokenizer;
import proguard.resources.file.ResourceJavaReference;

/**
 * This {@link DataEntryReader} collects the java references in a resource file and adds them to the references field.
 */
public class ResourceJavaReferenceCollector
    implements DataEntryReader
{
    private Set<ResourceJavaReference> references;


    public Set<ResourceJavaReference> getReferences()
    {
        return references;
    }


    @Override
    public void read(DataEntry dataEntry) throws IOException
    {
        Set<ResourceJavaReference> set = new HashSet<>();

        Reader reader =
            new BufferedReader(
                new InputStreamReader(dataEntry.getInputStream()));

        try
        {
            DataEntryTokenizer tokenizer = new DataEntryTokenizer(reader);
            DataEntryToken     token;
            while ((token = tokenizer.nextToken()) != null)
            {
                if (token.type == DataEntryTokenType.JAVA_IDENTIFIER)
                {
                    set.add(new ResourceJavaReference(token.string));
                }
            }
        }
        finally
        {
            dataEntry.closeInputStream();
        }

        references = set;
    }
}
