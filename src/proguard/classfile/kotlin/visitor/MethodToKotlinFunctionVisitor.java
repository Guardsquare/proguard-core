/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
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
            new AllFunctionsVisitor(
            new KotlinFunctionFilter(
                func -> programMethod.equals(func.referencedMethod),
                kotlinFunctionVisitor)));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.kotlinMetadataAccept(
            new AllFunctionsVisitor(
            new KotlinFunctionFilter(
                func -> libraryMethod.equals(func.referencedMethod),
                kotlinFunctionVisitor)));
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField) {}

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField) {}
}
