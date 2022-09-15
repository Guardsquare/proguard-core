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
package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.SubclassAdder;
import proguard.classfile.visitor.*;

import java.util.*;

/**
 * This ClassPoolVisitor and ClassVisitor fills out the subclasses of all
 * classes (in the class pools) that it visits.
 * <p>
 * It has a more efficient implementation as a ClassPoolVisitor. You then
 * must be careful to create a single instance and apply it to subclass
 * pools first; for example first the program class pool, then the
 * underlying library class pool.
 * <p>
 * This class is *NOT* thread-safe.
 *
 * @author Eric Lafortune
 */
public class ClassSubHierarchyInitializer
implements   ClassPoolVisitor,
             ClassVisitor
{
    private final Map<Clazz, Set<Clazz>> subClassesMap = new HashMap<>();


    // Implementations for ClassPoolVisitor.

    public void visitClassPool(ClassPool classPool)
    {
        // Optimized implementation. Only this implementation supports proper
        // sub-hierarchy re-initialization.

        // Collect the subclass information for all classes.
        classPool.classesAccept(new MySubclassCollector());

        // Store the cached subclasses all classes.
        classPool.classesAccept(new MySubclassSetter());
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
        // Direct implementation.

        // Add this class to the subclasses of its superclass and interfaces
        // (through their class constants).
        ConstantVisitor subClassCollector =
            new ReferencedClassVisitor(
            new SubclassAdder(programClass));

        programClass.superClassConstantAccept(subClassCollector);
        programClass.interfaceConstantsAccept(subClassCollector);
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Direct implementation.

        // Add this class to the subclasses of its superclass and interfaces.
        ClassVisitor subClassCollector =
            new SubclassAdder(libraryClass);

        libraryClass.superClassAccept(subClassCollector);
        libraryClass.interfacesAccept(subClassCollector);
    }


    // Small utility classes.

    /**
     * This ClassVisitor collects (in the subclasses map) the subclasses of
     * the classes that it visits.
     */
    private class MySubclassCollector
    implements    ClassVisitor
    {
        // Implementations for ClassVisitor.

        @Override
        public void visitAnyClass(Clazz clazz)
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " does not support " + clazz.getClass().getName());
        }


        @Override
        public void visitProgramClass(ProgramClass programClass)
        {
            // Add this class to the subclasses of its superclass and interfaces
            // (through their class constants).
            ConstantVisitor subClassCollector =
                new ReferencedClassVisitor(
                new MySubclassAdder(programClass));

            programClass.superClassConstantAccept(subClassCollector);
            programClass.interfaceConstantsAccept(subClassCollector);
        }


        @Override
        public void visitLibraryClass(LibraryClass libraryClass)
        {
            // Add this class to the subclasses of its superclass and interfaces.
            ClassVisitor subClassCollector =
                new MySubclassAdder(libraryClass);

            libraryClass.superClassAccept(subClassCollector);
            libraryClass.interfacesAccept(subClassCollector);
        }
    }


    /**
     * This ClassVisitor adds a given subclass to the sets of the classes that
     * it visits.
     */
    private class MySubclassAdder
    implements    ClassVisitor
    {
        private final Clazz subClass;


        public MySubclassAdder(Clazz subClass)
        {
            this.subClass = subClass;
        }


        // Implementations for ClassVisitor.

        @Override
        public void visitAnyClass(Clazz clazz)
        {
            // Retrieve or create the collected set of subclasses.
            Set<Clazz> subClasses = subClassesMap.computeIfAbsent(clazz, newClass -> new LinkedHashSet<>());

            // Add the subclass.
            subClasses.add(subClass);
        }
    }



    /**
     * This ClassVisitor sets (from the subclasses map) the collected sets
     * of the subclasses to the classes that it visits.
     */
    private class MySubclassSetter
    implements    ClassVisitor
    {
        // Implementations for ClassVisitor.

        @Override
        public void visitAnyClass(Clazz clazz)
        {
            throw new UnsupportedOperationException(this.getClass().getName() + " does not support " + clazz.getClass().getName());
        }


        @Override
        public void visitProgramClass(ProgramClass programClass)
        {
            // Retrieve the collected set of subclasses.
            Set<Clazz> subClasses = subClassesMap.get(programClass);
            if (subClasses != null)
            {
                // Set it to the class, as an array.
                Clazz[] subClassesArray = subClasses.toArray(new Clazz[0]);

                programClass.subClasses    = subClassesArray;
                programClass.subClassCount = subClassesArray.length;
            }
            else
            {
                programClass.subClasses    = new Clazz[0];
                programClass.subClassCount = 0;
            }
        }


        @Override
        public void visitLibraryClass(LibraryClass libraryClass)
        {
            // Retrieve the collected set of subclasses.
            Set<Clazz> subClasses = subClassesMap.get(libraryClass);
            if (subClasses != null)
            {
                // Set it to the class, as an array.
                Clazz[] subClassesArray = subClasses.toArray(new Clazz[0]);

                libraryClass.subClasses    = subClassesArray;
                libraryClass.subClassCount = subClassesArray.length;
            }
            else
            {
                libraryClass.subClasses    = new Clazz[0];
                libraryClass.subClassCount = 0;
            }
        }
    }
}
