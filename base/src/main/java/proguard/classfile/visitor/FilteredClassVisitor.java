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
package proguard.classfile.visitor;

import proguard.classfile.ClassPool;
import proguard.util.ClassNameParser;
import proguard.util.ListParser;
import proguard.util.StringMatcher;


/**
 * This {@link ClassPoolVisitor} lets a given {@link ClassVisitor} visit all matching Clazz
 * instances of the class pools it visits.
 *
 * @author Joren Van Hecke
 */
public class FilteredClassVisitor implements ClassPoolVisitor
{
    private final ClassVisitor classVisitor;

    private final StringMatcher classNameFilter;


    /**
     * Creates a new FilteredClassVisitor with the given class name filter and visitor.
     * @param classNameFilter the regular expression that is used to filter the classes
     *                        of a {@link ClassPool} that must be visited. If the value of
     *                        {@code classNameFilter} is null, then no classes can possibly
     *                        match, so this instance will never visit any {@link ClassPool}.
     * @param classVisitor    the visitor that is passed along when a {@link ClassPool} is visited.
     */
    public FilteredClassVisitor(String classNameFilter, ClassVisitor classVisitor)
    {
        this(classNameFilter == null ? null : new ListParser(new ClassNameParser()).parse(classNameFilter), classVisitor);
    }

    /**
     * Creates a new FilteredClassVisitor with the given class name filter and visitor.
     * @param classNameFilter the filter that is used to filter the classes
     *                        of a {@link ClassPool} that must be visited. If the value of
     *                        {@code classNameFilter} is null, then no classes can possibly
     *                        match, so this instance will never visit any {@link ClassPool}.
     * @param classVisitor    the visitor that is passed along when a {@link ClassPool} is visited.
     */
    public FilteredClassVisitor(StringMatcher classNameFilter, ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
        this.classNameFilter = classNameFilter;
    }


    @Override
    public void visitClassPool(ClassPool classPool)
    {
        if (classNameFilter != null)
        {
            classPool.classesAccept(classNameFilter, classVisitor);
        }
    }
}
