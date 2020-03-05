/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect;

import proguard.classfile.Clazz;
import proguard.classfile.kotlin.*;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

/**
 * TODO supposedly local variable references are not supported yet.
 *
 * @author James Hamilton
 */
public class LocalVariableReferenceInfo
implements   CallableReferenceInfo
{
    private final Clazz                              ownerClass;
    private final KotlinDeclarationContainerMetadata ownerMetadata;
    private final String                             name;
    private final String                             signature;

    public LocalVariableReferenceInfo(Clazz                              ownerClass,
                                      KotlinDeclarationContainerMetadata ownerMetadata,
                                      String                             name,
                                      String                             signature)
    {
        this.ownerClass    = ownerClass;
        this.ownerMetadata = ownerMetadata;
        this.name          = name;
        this.signature     = signature;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getSignature()
    {
        return this.signature;
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return this.ownerMetadata;
    }

    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitLocalVariableReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.ownerMetadata.accept(this.ownerClass, kotlinMetadataVisitor);
    }
}
