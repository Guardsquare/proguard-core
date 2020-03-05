/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2019 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect;

import proguard.classfile.kotlin.KotlinDeclarationContainerMetadata;
import proguard.classfile.kotlin.visitor.KotlinMetadataVisitor;
import proguard.classfile.kotlin.reflect.visitor.CallableReferenceInfoVisitor;

/**
 * Information about callable references.
 *
 * @author James Hamilton
 */
public interface CallableReferenceInfo
{
    /**
     * The Kotlin name of the callable, the one which was declared in the source code (@JvmName doesn't change it).
     *
     * @return The Kotlin name.
     */
    String getName();

    /**
     * The signature of the callable.
     *
     * @return The signature.
     */
    String getSignature();


    /**
     * The class or package where the callable should be located, usually specified on the LHS of the '::' operator.
     *
     * Note: this is not necessarily the location where the callable is *declared* - it could be declared in
     * a superclass.
     *
     * @return The owner.
     */
    KotlinDeclarationContainerMetadata getOwner();

    void accept(CallableReferenceInfoVisitor callableReferenceInfoVisitor);

    void ownerAccept(KotlinMetadataVisitor kotlinMetadataVisitor);
}
