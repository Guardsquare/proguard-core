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

package proguard.classfile.kotlin.visitor;

import proguard.classfile.*;
import proguard.classfile.kotlin.visitor.filter.KotlinPropertyFilter;
import proguard.classfile.visitor.MemberVisitor;

/**
 * Apply the given {@link KotlinPropertyVisitor} if the member is
 * a backing field, getter or setter for a property.
 *
 * @author James Hamilton
 */
public class MemberToKotlinPropertyVisitor
implements   MemberVisitor
{

    private final KotlinPropertyVisitor kotlinPropertyVisitor;


    public MemberToKotlinPropertyVisitor(KotlinPropertyVisitor kotlinPropertyVisitor)
    {
        this.kotlinPropertyVisitor = kotlinPropertyVisitor;
    }


    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        programClass.kotlinMetadataAccept(
            new AllPropertyVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedBackingField == programField,
                    this.kotlinPropertyVisitor)));
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        libraryClass.kotlinMetadataAccept(
            new AllPropertyVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedBackingField == libraryField,
                    this.kotlinPropertyVisitor)));
    }

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programClass.kotlinMetadataAccept(
            new AllPropertyVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedGetterMethod == programMethod ||
                            prop.referencedSetterMethod == programMethod,
                    this.kotlinPropertyVisitor)));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.kotlinMetadataAccept(
            new AllPropertyVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedGetterMethod == libraryMethod ||
                            prop.referencedSetterMethod == libraryMethod,
                    this.kotlinPropertyVisitor)));
    }
}
