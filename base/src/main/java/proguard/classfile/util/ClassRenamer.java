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
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.editor.ConstantPoolEditor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;

import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * This <code>ClassVisitor</code> renames the class names and class member
 * names of the classes it visits.
 *
 * @author Eric Lafortune
 */
public class ClassRenamer
implements   ClassVisitor,
             MemberVisitor,
             ConstantVisitor
{
    private final Function<Clazz, String>           classNameFunction;
    private final BiFunction<Clazz, Member, String> memberNameFunction;
    private final ClassVisitor                      extraClassVisitor;
    private final MemberVisitor                     extraMemberVisitor;


    public ClassRenamer(Function<Clazz, String> classNameFunction)
    {
        this(classNameFunction, (clazz, member) -> member.getName(clazz), null, null);
    }


    public ClassRenamer(Function<Clazz, String> classNameFunction, BiFunction<Clazz, Member, String> memberNameFunction)
    {
        this(classNameFunction, memberNameFunction, null, null);
    }


    public ClassRenamer(Function<Clazz, String> classNameFunction,
                        ClassVisitor            extraClassVisitor,
                        MemberVisitor           extraMemberVisitor)
    {
        this(classNameFunction, (clazz, member) -> member.getName(clazz), extraClassVisitor, extraMemberVisitor);
    }


    public ClassRenamer(Function<Clazz, String>           classNameFunction,
                        BiFunction<Clazz, Member, String> memberNameFunction,
                        ClassVisitor                      extraClassVisitor,
                        MemberVisitor                     extraMemberVisitor)
    {
        this.classNameFunction  = classNameFunction;
        this.memberNameFunction = memberNameFunction;
        this.extraClassVisitor  = extraClassVisitor;
        this.extraMemberVisitor = extraMemberVisitor;
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
        // Rename this class.
        programClass.thisClassConstantAccept(this);

        // Rename the class members.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);
    }


    @Override
    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Has the library class name changed?
        String name    = libraryClass.getName();
        String newName = this.classNameFunction.apply(libraryClass);
        if (newName != null && !newName.equals(name))
        {
            libraryClass.thisClassName = newName;

            if (extraClassVisitor != null)
            {
                extraClassVisitor.visitLibraryClass(libraryClass);
            }
        }

        // Rename the class members.
        libraryClass.fieldsAccept(this);
        libraryClass.methodsAccept(this);
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


    // Implementations for ConstantVisitor.

    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Update the Class entry if required.
        String name    = clazz.getName();
        String newName = classNameFunction.apply(clazz);
        if (newName != null && !newName.equals(name))
        {
            // Refer to a new Utf8 entry.
            classConstant.u2nameIndex =
                new ConstantPoolEditor((ProgramClass)clazz).addUtf8Constant(newName);

            if (extraClassVisitor != null)
            {
                clazz.accept(extraClassVisitor);
            }
        }
    }
}
