/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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
import proguard.classfile.LibraryClass;
import proguard.classfile.LibraryMethod;
import proguard.classfile.Member;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;

/**
 * Returns the last referenced class of referencedClasses from the program-/ librarymethod.
 */
public class ReturnClassExtractor
    implements MemberVisitor
{

    public Clazz returnClass;

    @Override
    public void visitAnyMember(Clazz clazz, Member member)
    {
        // only interested in program and librarymethods
    }

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        if (programMethod.referencedClasses != null)
        {
            this.returnClass = programMethod.referencedClasses[programMethod.referencedClasses.length - 1];
        }
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        if (libraryMethod.referencedClasses != null)
        {
            this.returnClass = libraryMethod.referencedClasses[libraryMethod.referencedClasses.length - 1];
        }
    }
}
