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
package proguard.resources.file.visitor;

import proguard.resources.file.ResourceFile;

import proguard.util.*;

import java.util.List;

/**
 * Delegates to another {@link ResourceFileVisitor}, but only if the visited file's name matches a given filter.
 *
 * @author Johan Leys
 */
public class ResourceFileNameFilter
implements   ResourceFileVisitor
{
    private final StringMatcher       fileNameFilter;
    private final ResourceFileVisitor acceptedVisitor;
    private final ResourceFileVisitor rejectedVisitor;


    public ResourceFileNameFilter(String              fileNameRegularExpression,
                                  ResourceFileVisitor acceptedVisitor)
    {
        this (new ListParser(new FileNameParser()).parse(fileNameRegularExpression), acceptedVisitor);
    }


    public ResourceFileNameFilter(StringMatcher       fileNameFilter,
                                  ResourceFileVisitor acceptedVisitor)
    {
        this(fileNameFilter, acceptedVisitor, null);
    }


    public ResourceFileNameFilter(List                regularExpressions,
                                  ResourceFileVisitor acceptedVisitor)
    {
        this(regularExpressions, acceptedVisitor, null);
    }


    public ResourceFileNameFilter(String              fileNameRegularExpression,
                                  ResourceFileVisitor acceptedVisitor,
                                  ResourceFileVisitor rejectedVisitor)
    {
        this (new ListParser(new FileNameParser()).parse(fileNameRegularExpression), acceptedVisitor, rejectedVisitor);
    }


    public ResourceFileNameFilter(List                regularExpressions,
                                  ResourceFileVisitor acceptedVisitor,
                                  ResourceFileVisitor rejectedVisitor)
    {
        this(new ListParser(new FileNameParser()).parse(regularExpressions), acceptedVisitor, rejectedVisitor);
    }


    public ResourceFileNameFilter(StringMatcher       fileNameFilter,
                                  ResourceFileVisitor acceptedVisitor,
                                  ResourceFileVisitor rejectedVisitor)
    {
        this.fileNameFilter  = fileNameFilter;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }


    // Implementations for ResourceFileVisitor.

    @Override
    public void visitAnyResourceFile(ResourceFile resourceFile)
    {
        ResourceFileVisitor delegate = getDelegate(resourceFile);
        if (delegate != null)
        {
            resourceFile.accept(delegate);
        }
    }


    // Small utility methods.

    private ResourceFileVisitor getDelegate(ResourceFile resourceFile)
    {
        return fileNameFilter.matches(resourceFile.fileName) ?
            acceptedVisitor : rejectedVisitor;
    }
}
