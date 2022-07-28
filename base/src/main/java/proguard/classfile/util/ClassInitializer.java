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
import proguard.classfile.visitor.*;

/**
 * This {@link ClassVisitor} initializes the class hierarchy and references of
 * all classes that it visits. It assumes that the class hierarchies of the
 * classes to which it refers have already been initialized. Otherwise, you
 * need to call subsequently call {@link ClassSuperHierarchyInitializer},
 * {@link ClassSubHierarchyInitializer}, and {@link ClassReferenceInitializer}
 * on all classes.
 *
 * <p/>
 * This visitor optionally prints warnings if some items can't be found.
 * <p/>
 *
 * @see ClassSuperHierarchyInitializer
 * @see ClassSubHierarchyInitializer
 * @see ClassReferenceInitializer
 *
 * @author Eric Lafortune
 */
public class ClassInitializer
implements   ClassVisitor
{
    private final ClassSuperHierarchyInitializer classSuperHierarchyInitializer;
    private final ClassSubHierarchyInitializer   classSubHierarchyInitializer;
    private final ClassReferenceInitializer      classReferenceInitializer;


    /**
     * Creates a new ClassInitializer that initializes the class hierarchies
     * and references of all visited class files.
     */
    public ClassInitializer(ClassPool programClassPool,
                            ClassPool libraryClassPool)
    {
        this(programClassPool,
             libraryClassPool,
             null,
             null,
             null,
             null);
    }


    /**
     * Creates a new ClassInitializer that initializes the class hierarchies
     * and references of all visited class files, optionally printing warnings
     * if some classes or class members can't be found or if they are in the
     * program class pool.
     */
    public ClassInitializer(ClassPool      programClassPool,
                            ClassPool      libraryClassPool,
                            WarningPrinter missingClassWarningPrinter,
                            WarningPrinter missingProgramMemberWarningPrinter,
                            WarningPrinter missingLibraryMemberWarningPrinter,
                            WarningPrinter dependencyWarningPrinter)
    {
        this(programClassPool,
             libraryClassPool,
             true,
             missingClassWarningPrinter,
             missingProgramMemberWarningPrinter,
             missingLibraryMemberWarningPrinter,
             dependencyWarningPrinter);
    }


    /**
     * Creates a new ClassInitializer that initializes the references
     * of all visited class files, optionally printing warnings if some classes
     * or class members can't be found or if they are in the program class pool.
     */
    public ClassInitializer(ClassPool      programClassPool,
                            ClassPool      libraryClassPool,
                            boolean        checkAccessRules,
                            WarningPrinter missingClassWarningPrinter,
                            WarningPrinter missingProgramMemberWarningPrinter,
                            WarningPrinter missingLibraryMemberWarningPrinter,
                            WarningPrinter dependencyWarningPrinter)
    {
        this.classSuperHierarchyInitializer = new ClassSuperHierarchyInitializer(programClassPool,
                                                                                 libraryClassPool,
                                                                                 missingClassWarningPrinter,
                                                                                 dependencyWarningPrinter);
        this.classSubHierarchyInitializer   = new ClassSubHierarchyInitializer();
        this.classReferenceInitializer      = new ClassReferenceInitializer(programClassPool,
                                                                            libraryClassPool,
                                                                            missingClassWarningPrinter,
                                                                            missingProgramMemberWarningPrinter,
                                                                            missingLibraryMemberWarningPrinter,
                                                                            dependencyWarningPrinter);
    }

    // Implementations for ClassVisitor.

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        // Initialize all references to/from the class.
        clazz.accept(classSuperHierarchyInitializer);
        clazz.accept(classSubHierarchyInitializer);
        clazz.accept(classReferenceInitializer);
    }
}
