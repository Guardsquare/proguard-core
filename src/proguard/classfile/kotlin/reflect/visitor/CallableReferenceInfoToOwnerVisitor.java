/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect.visitor;

import proguard.classfile.kotlin.reflect.CallableReferenceInfo;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;

/**
 * @author James Hamilton
 */
public class CallableReferenceInfoToOwnerVisitor
implements CallableReferenceInfoVisitor
{
    private final KotlinMetadataVisitor kotlinMetadataVisitor;


    public CallableReferenceInfoToOwnerVisitor(KotlinMetadataVisitor kotlinMetadataVisitor)
    {
        this.kotlinMetadataVisitor = kotlinMetadataVisitor;
    }

    @Override
    public void visitAnyCallableReferenceInfo(CallableReferenceInfo callableReferenceInfo)
    {
       callableReferenceInfo.ownerAccept(kotlinMetadataVisitor);
    }
}
