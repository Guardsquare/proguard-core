/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */
package proguard.util;

import proguard.classfile.*;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.visitor.*;
import proguard.resources.file.ResourceFile;
import proguard.resources.file.visitor.ResourceFileVisitor;

/**
 *  This interface defines visitor methods for the main Processable implementations.
 */
public interface ProcessableVisitor
extends          ClassVisitor,
                 MemberVisitor,
                 AttributeVisitor,
                 ResourceFileVisitor
{
    /**
     * Visits any Processable instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyProcessable(Processable processable)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+processable.getClass().getName());
    }


    // Implementations for ClassVisitor.

    default void visitAnyClass(Clazz clazz)
    {
        visitAnyProcessable(clazz);
    }


    // Implementations for MemberVisitor.

    default void visitAnyMember(Clazz clazz, Member member)
    {
        visitAnyProcessable(member);
    }


    // Implementations for AttributeVisitor.

    default void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
        visitAnyProcessable(attribute);
    }


    // Implementations for ResourceFileVisitor.

    default void visitAnyResourceFile(ResourceFile resourceFile)
    {
        visitAnyProcessable(resourceFile);
    }
}
