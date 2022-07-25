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
import proguard.classfile.util.AccessUtil;

/**
 * This {@link MemberVisitor} sets the access part of the access flags of
 * the program class members that its visits to a given value.
 *
 * @see ClassConstants
 *
 * @author Eric Lafortune
 */
public class MemberAccessSetter
implements   MemberVisitor
{
    private final int accessFlags;


    /**
     * Creates a new MemberAccessSetter.
     * @param accessFlags the member access flags to be set.
     */
    public MemberAccessSetter(int accessFlags)
    {
        this.accessFlags = accessFlags;
    }


    // Implementations for MemberVisitor.

    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField) {}
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod) {}


    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        programField.u2accessFlags =
            AccessUtil.replaceAccessFlags(programField.u2accessFlags, accessFlags);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programMethod.u2accessFlags =
            AccessUtil.replaceAccessFlags(programMethod.u2accessFlags, accessFlags);
    }
}
