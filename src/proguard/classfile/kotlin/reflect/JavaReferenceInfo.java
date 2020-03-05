/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect;

import proguard.classfile.*;
import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

/**
 * @author James Hamilton
 */
public abstract class JavaReferenceInfo
implements            CallableReferenceInfo
{
    private final   Clazz  ownerClass;
    protected final Clazz  clazz;
    protected final Member member;

    public JavaReferenceInfo(Clazz ownerClass, Clazz clazz, Member member)
    {
        this.ownerClass = ownerClass;
        this.clazz      = clazz;
        this.member     = member;
    }

    @Override
    public String getName()
    {
        return this.member.getName(this.clazz);
    }

    @Override
    public String getSignature()
    {
        return this.getName() + this.member.getDescriptor(this.clazz);
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return null;
    }

    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitJavaReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        // There's no owner.
    }
}
