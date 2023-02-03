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
package proguard.classfile.util;

import proguard.classfile.Clazz;
import proguard.classfile.LibraryClass;
import proguard.classfile.LibraryMember;
import proguard.classfile.Member;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMember;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * This <code>MemberVisitor</code> renames the class member
 * names of the classes it visits.
 */
public class MemberRenamer
implements MemberVisitor
{
    private final BiFunction<Clazz, Member, String> memberNameFunction;
    private final MemberVisitor                     extraMemberVisitor;


    public MemberRenamer(BiFunction<Clazz, Member, String> memberNameFunction)
    {
        this(memberNameFunction, null);
    }


    public MemberRenamer(Function<Clazz, String> classNameFunction,
                         MemberVisitor           extraMemberVisitor)
    {
        this((clazz, member) -> member.getName(clazz),extraMemberVisitor);
    }


    public MemberRenamer(BiFunction<Clazz, Member, String> memberNameFunction,
                         MemberVisitor                     extraMemberVisitor)
    {
        this.memberNameFunction = memberNameFunction;
        this.extraMemberVisitor = extraMemberVisitor;
    }


    // Implementations for MemberVisitor.

    public void visitProgramMember(ProgramClass  programClass,
                                   ProgramMember programMember)
    {
        // Has the class member name changed?
        String name    = programMember.getName(programClass);
        String newName = memberNameFunction.apply(programClass, programMember);
        if (newName != null && !newName.equals(name))
        {
            programMember.u2nameIndex =
                new ConstantPoolEditor(programClass).addUtf8Constant(newName);

            if (extraMemberVisitor != null)
            {
                programMember.accept(programClass, extraMemberVisitor);
            }
        }
    }

    public void visitLibraryMember(LibraryClass  libraryClass,
                                   LibraryMember libraryMember)
    {
        // Has the library member name changed?
        String name    = libraryMember.getName(libraryClass);
        String newName = memberNameFunction.apply(libraryClass, libraryMember);
        if (newName != null && !newName.equals(name))
        {
            libraryMember.name = newName;

            if (extraMemberVisitor != null)
            {
                libraryMember.accept(libraryClass, extraMemberVisitor);
            }
        }
    }
}
