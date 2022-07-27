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
import proguard.classfile.kotlin.KotlinFunctionMetadata;
import proguard.classfile.kotlin.visitor.filter.KotlinFunctionFilter;
import proguard.classfile.visitor.MemberVisitor;

/**
 * Apply the given function visitor to a referenced method's corresponding
 * {@link KotlinFunctionMetadata}.
 *
 * @author James Hamilton
 */
public class MethodToKotlinFunctionVisitor
implements   MemberVisitor
{
    private final KotlinFunctionVisitor kotlinFunctionVisitor;


    public MethodToKotlinFunctionVisitor(KotlinFunctionVisitor kotlinFunctionVisitor)
    {
        this.kotlinFunctionVisitor = kotlinFunctionVisitor;
    }

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programClass.kotlinMetadataAccept(
            new AllFunctionVisitor(
            new KotlinFunctionFilter(
                func -> programMethod.equals(func.referencedMethod),
                kotlinFunctionVisitor)));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.kotlinMetadataAccept(
            new AllFunctionVisitor(
            new KotlinFunctionFilter(
                func -> libraryMethod.equals(func.referencedMethod),
                kotlinFunctionVisitor)));
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField) {}

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField) {}
}
