/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
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
            new AllConstructorsVisitor(
            new KotlinConstructorFilter(
                func -> programMethod.equals(func.referencedMethod),
                kotlinConstructorVisitor)));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.kotlinMetadataAccept(
            new AllConstructorsVisitor(
            new KotlinConstructorFilter(
                func -> libraryMethod.equals(func.referencedMethod),
                kotlinConstructorVisitor)));
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField) {}

    @Override
    public void visitProgramField(ProgramClass programClass, ProgramField programField) {}
}
