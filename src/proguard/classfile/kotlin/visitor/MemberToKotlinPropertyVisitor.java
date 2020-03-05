/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
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
            new AllKotlinPropertiesVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedBackingField == programField,
                    this.kotlinPropertyVisitor)));
    }

    @Override
    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        libraryClass.kotlinMetadataAccept(
            new AllKotlinPropertiesVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedBackingField == libraryField,
                    this.kotlinPropertyVisitor)));
    }

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programClass.kotlinMetadataAccept(
            new AllKotlinPropertiesVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedGetterMethod == programMethod ||
                            prop.referencedSetterMethod == programMethod,
                    this.kotlinPropertyVisitor)));
    }

    @Override
    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryClass.kotlinMetadataAccept(
            new AllKotlinPropertiesVisitor(
                new KotlinPropertyFilter(
                    prop -> prop.referencedGetterMethod == libraryMethod ||
                            prop.referencedSetterMethod == libraryMethod,
                    this.kotlinPropertyVisitor)));
    }
}
