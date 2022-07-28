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

import proguard.classfile.*;


/**
 * This {@link ClassVisitor} delegates its visits to one of two other given
 * {@link ClassVisitor} instances, depending on whether they have any
 * subclasses or not.
 *
 * @author Eric Lafortune
 */
public class BottomClassFilter
implements   ClassVisitor
{
    private final ClassVisitor bottomClassVisitor;
    private final ClassVisitor otherClassVisitor;

    /**
     * Creates a new BottomClassFilter.
     * @param bottomClassVisitor the <code>ClassVisitor</code> to which visits
     *                           to bottom classes will be delegated.
     */
    public BottomClassFilter(ClassVisitor bottomClassVisitor)
    {
        this(bottomClassVisitor, null);
    }


    /**
     * Creates a new BottomClassFilter.
     * @param bottomClassVisitor the <code>ClassVisitor</code> to which visits
     *                           to bottom classes will be delegated.
     * @param otherClassVisitor  the <code>ClassVisitor</code> to which visits
     *                           to non-bottom classes will be delegated.
     */
    public BottomClassFilter(ClassVisitor bottomClassVisitor,
                             ClassVisitor otherClassVisitor)
    {
        this.bottomClassVisitor = bottomClassVisitor;
        this.otherClassVisitor  = otherClassVisitor;
    }


    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not support " + clazz.getClass().getName());
    }


    @Override
    public void visitProgramClass(ProgramClass programClass)
    {
        // Is this a bottom class in the class hierarchy?
        ClassVisitor classVisitor = programClass.subClassCount == 0 ?
            bottomClassVisitor :
            otherClassVisitor;

        if (classVisitor != null)
        {
            classVisitor.visitProgramClass(programClass);
        }
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Is this a bottom class in the class hierarchy?
        ClassVisitor classVisitor = libraryClass.subClassCount == 0 ?
            bottomClassVisitor :
            otherClassVisitor;

        if (classVisitor != null)
        {
            classVisitor.visitLibraryClass(libraryClass);
        }
    }
}
