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
import proguard.classfile.kotlin.KotlinConstructorMetadata;
import proguard.classfile.kotlin.visitor.filter.KotlinConstructorFilter;
import proguard.classfile.visitor.MemberVisitor;

/**
 * Apply the given function visitor to a referenced constructors's corresponding
 * {@link KotlinConstructorMetadata}.
 *
 * @author James Hamilton
 */
public class MethodToKotlinConstructorVisitor
implements   MemberVisitor
{
    private final KotlinConstructorVisitor kotlinConstructorVisitor;


    public MethodToKotlinConstructorVisitor(KotlinConstructorVisitor kotlinConstructorVisitor)
    {
        this.kotlinConstructorVisitor = kotlinConstructorVisitor;
    }

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programClass.kotlinMetadataAccept(
            new AllConstructorVisitor(
            new KotlinConstructorFilter(
                func -> programMethod.equals(func.referencedMethod),
                kotlinConstructorVisitor)));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.kotlinMetadataAccept(
            new AllConstructorVisitor(
            new KotlinConstructorFilter(
                func -> libraryMethod.equals(func.referencedMethod),
                kotlinConstructorVisitor)));
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField) {}

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField) {}
}
