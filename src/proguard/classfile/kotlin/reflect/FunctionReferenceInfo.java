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
 * FunctionReference info.
 *
 * @author James Hamilton
 */
public class FunctionReferenceInfo
implements   CallableReferenceInfo
{
    private final Clazz                              ownerClass;
    private final KotlinDeclarationContainerMetadata ownerMetadata;
    private final KotlinFunctionMetadata             functionMetadata;

    public FunctionReferenceInfo(Clazz                              ownerClass,
                                 KotlinDeclarationContainerMetadata ownerMetadata,
                                 KotlinFunctionMetadata             functionMetadata)
    {
        this.ownerClass       = ownerClass;
        this.functionMetadata = functionMetadata;
        this.ownerMetadata    = ownerMetadata;
    }

    @Override
    public String getName()
    {
        return functionMetadata.name;
    }

    /**
     * For functions this is just the jvmSignature.
     *
     * @return The JVM signature.
     */
    @Override
    public String getSignature()
    {
        return functionMetadata.jvmSignature.asString();
    }


    @Override
    public KotlinDeclarationContainerMetadata getOwner()
    {
        return this.ownerMetadata;
    }


    @Override
    public void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor)
    {
        callableReferenceInfoVisitor.visitFunctionReferenceInfo(this);
    }

    @Override
    public void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.ownerMetadata.accept(this.ownerClass, kotlinMetadataVisitor);
    }
}
