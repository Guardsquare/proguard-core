/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect.visitor;

import proguard.classfile.kotlin.reflect.*;

/**
 * @author James Hamilton
 */
public interface CallableReferenceInfoVisitor
{
    void visitAnyCallableReferenceInfo(CallableReferenceInfo callableReferenceInfo);

    default void visitFunctionReferenceInfo(FunctionReferenceInfo functionReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(functionReferenceInfo);
    }

    default void visitJavaReferenceInfo(JavaReferenceInfo javaReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(javaReferenceInfo);
    }

    default void visitLocalVariableReferenceInfo(LocalVariableReferenceInfo localVariableReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(localVariableReferenceInfo);
    }

    default void visitPropertyReferenceInfo(PropertyReferenceInfo propertyReferenceInfo)
    {
        this.visitAnyCallableReferenceInfo(propertyReferenceInfo);
    }
}
