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
 * {@link Clazz} instances.
 *
 * @author Eric Lafortune
 */
public interface ClassVisitor
{
    /**
     * Visits any Clazz instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    void visitAnyClass(Clazz clazz);


    default void visitProgramClass(ProgramClass programClass)
    {
        visitAnyClass(programClass);
    }


    default void visitLibraryClass(LibraryClass libraryClass)
    {
        visitAnyClass(libraryClass);
    }
}
