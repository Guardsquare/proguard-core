/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 GuardSquare NV
 */

package proguard.classfile.kotlin.reflect;

import proguard.classfile.*;

import static proguard.classfile.util.kotlin.KotlinNameUtil.generateGetterName;

/**
 * @author James Hamilton
 */
public class JavaFieldReferenceInfo
extends      JavaReferenceInfo
{
    public JavaFieldReferenceInfo(Clazz ownerClass, Clazz clazz, Member member)
    {
        super(ownerClass, clazz, member);
    }

    /**
     * If there is no getter method then the signature is the imaginary default getter that
     * would be generated otherwise e.g. "myProperty" -> "getMyProperty".
     *
     * @return the signature.
     */
    @Override
    public String getSignature()
    {
        return generateGetterName(this.getName()) + "()" + this.member.getDescriptor(this.clazz);
    }
}
