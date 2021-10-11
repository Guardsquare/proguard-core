/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.resources.file.visitor;

import proguard.io.*;
import proguard.resources.file.*;

import java.io.*;
import java.util.*;

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
