/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Member;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramMethod;
import proguard.util.Counter;

/**
 * This {@link MemberVisitor} counts the number of methods that have been visited.
 *
 * @author Ruben Pieters
 */
public class MethodCounter
implements   MemberVisitor,
             Counter
{
    private int count;


    // Implementations for Counter.

    @Override
    public int getCount()
    {
        return count;
    }


    // Implementations for MemberVisitor.

    @Override
    public void visitAnyMember(Clazz clazz, Member member) {}

    @Override
    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        count++;
    }
}
