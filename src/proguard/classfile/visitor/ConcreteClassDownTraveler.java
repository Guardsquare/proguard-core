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
 * This {@link ClassVisitor} lets a given {@link ClassVisitor}
 * travel to the first concrete subclasses down in its hierarchy of abstract
 * classes and concrete classes.
 *
 * @author Eric Lafortune
 */
public class ConcreteClassDownTraveler
implements   ClassVisitor
{
    private final ClassVisitor classVisitor;


    /**
     * Creates a new ConcreteClassDownTraveler.
     * @param classVisitor     the <code>ClassVisitor</code> to
     *                         which visits will be delegated.
     */
    public ConcreteClassDownTraveler(ClassVisitor classVisitor)
    {
        this.classVisitor = classVisitor;
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
        // Is this an abstract class or an interface?
        if ((programClass.getAccessFlags() &
             (AccessConstants.INTERFACE |
              AccessConstants.ABSTRACT)) != 0)
        {
            // Travel down the hierarchy.
            int     subClassCount = programClass.subClassCount;
            Clazz[] subClasses    = programClass.subClasses;
            for (int index = 0; index < subClassCount; index++)
            {
                subClasses[index].accept(this);
            }
        }
        else
        {
            // Visit the class. Don't descend any further.
            programClass.accept(classVisitor);
        }
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Is this an abstract class or interface?
        if ((libraryClass.getAccessFlags() &
             (AccessConstants.INTERFACE |
              AccessConstants.ABSTRACT)) != 0)
        {
            // Travel down the hierarchy.
            int     subClassCount = libraryClass.subClassCount;
            Clazz[] subClasses    = libraryClass.subClasses;
            for (int index = 0; index < subClassCount; index++)
            {
                subClasses[index].accept(this);
            }
        }
        else
        {
            // Visit the class. Don't descend any further.
            libraryClass.accept(classVisitor);
        }
    }
}
