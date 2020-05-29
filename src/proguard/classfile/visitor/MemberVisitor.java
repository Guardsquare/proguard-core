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
 * This interface specifies the methods for a visitor of
 * {@link ProgramMember} instances and {@link LibraryMember}
 * instances.
 *
 * @author Eric Lafortune
 */
public interface MemberVisitor
{
    /**
     * Visits any Member instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyMember(Clazz clazz, Member member)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+member.getClass().getName());
    }



    default void visitProgramMember(ProgramClass programClass, ProgramMember programMember)
    {
        visitAnyMember(programClass, programMember);
    }


    default void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        visitProgramMember(programClass, programField);
    }


    default void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        visitProgramMember(programClass, programMethod);
    }



    default void visitLibraryMember(LibraryClass libraryClass, LibraryMember libraryMember)
    {
        visitAnyMember(libraryClass, libraryMember);
    }


    default void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        visitLibraryMember(libraryClass, libraryField);
    }


    default void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        visitLibraryMember(libraryClass, libraryMethod);
    }
}
