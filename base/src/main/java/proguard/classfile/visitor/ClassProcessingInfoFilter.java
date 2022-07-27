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

import proguard.classfile.Clazz;
import proguard.util.Processable;

import java.util.function.Predicate;

/**
 * Delegates all class visits to another given visitor, depending on if the given predicate passes or not.
 *
 * @author Johan Leys
 * @author James Hamilton
 */
public class ClassProcessingInfoFilter
implements ClassVisitor
{

    private final Predicate<Object> predicate;
    private final ClassVisitor acceptedClassVisitor;
    private final ClassVisitor rejectedClassVisitor;


    /**
     * Creates a new  ClassVisitorInfoFilter.
     * @param predicate            the visitor info predicate to check.
     * @param acceptedClassVisitor the class visitor for classes that have the
     *                             given visitor info.
     */
    public ClassProcessingInfoFilter(Predicate<Object> predicate,
                                     ClassVisitor acceptedClassVisitor)
    {
        this(predicate, acceptedClassVisitor, null);
    }


    /**
     * Creates a new  ClassVisitorInfoFilter that checks the identity of the
     * given visitor info.
     * @param predicate            the visitor info to check.
     * @param acceptedClassVisitor the class visitor for classes that have the
     *                             given visitor info.
     * @param rejectedClassVisitor the class visitor for classes that don't
     *                             have the given visitor info.
     */
    public ClassProcessingInfoFilter(Predicate<Object> predicate,
                                     ClassVisitor acceptedClassVisitor,
                                     ClassVisitor rejectedClassVisitor)
    {
        this.predicate            = predicate;
        this.acceptedClassVisitor = acceptedClassVisitor;
        this.rejectedClassVisitor = rejectedClassVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        ClassVisitor delegate = this.getDelegate(clazz);
        if (delegate != null)
        {
            clazz.accept(delegate);
        }
    }


    // Helper methods.

    private ClassVisitor getDelegate(Processable processable)
    {
        return this.predicate.test(processable.getProcessingInfo()) ? this.acceptedClassVisitor : this.rejectedClassVisitor;
    }
}
