/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.visitor;

import proguard.classfile.*;

/**
 * This {@link ClassVisitor} delegates its visits to given
 * acceptVisitor when the visited class has method with
 * given name and descriptor and delegates to rejectVisitor when
 * none of the class methods match.
 */
public class ClassMethodFilter
implements   ClassVisitor
{
    private final String       methodName;
    private final String       descriptor;
    private final ClassVisitor acceptVisitor;
    private final ClassVisitor rejectVisitor;

    public ClassMethodFilter(String       methodName,
                             String       descriptor,
                             ClassVisitor acceptVisitor,
                             ClassVisitor rejectVisitor)
    {
        this.methodName    = methodName;
        this.descriptor    = descriptor;
        this.acceptVisitor = acceptVisitor;
        this.rejectVisitor = rejectVisitor;
    }

    @Override
    public void visitAnyClass(Clazz clazz)
    {
        ClassVisitor delegateVisitor = delegateVisitor(clazz);
        if (delegateVisitor != null)
        {
            delegateVisitor.visitAnyClass(clazz);
        }
    }

    private ClassVisitor delegateVisitor(Clazz clazz)
    {
        Method method = clazz.findMethod(methodName, descriptor);
        return method != null ? acceptVisitor : rejectVisitor;
    }
}