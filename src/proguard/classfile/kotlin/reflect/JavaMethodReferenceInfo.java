/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect;

import proguard.classfile.*;

/**
 * @author James Hamilton
 */
public class JavaMethodReferenceInfo extends JavaReferenceInfo
{
    public JavaMethodReferenceInfo(Clazz ownerClass, Clazz clazz, Member member)
    {
        super(ownerClass, clazz, member);
    }
}
