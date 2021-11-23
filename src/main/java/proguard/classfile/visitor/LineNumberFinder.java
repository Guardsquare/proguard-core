/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
 */

package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.LineNumberTableAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;

/**
 * Retrieves the lineNumber for a given offset from a codeattribute.
 *
 * @author James Hamilton
 */
public class LineNumberFinder
    implements AttributeVisitor
{
    private final int offset;
    public int lineNumber = -1; // -1 == not found

    public LineNumberFinder(int offset)
    {
        this.offset = offset;
    }

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute)
    {
        // not interested in Attributes except LineNumberTableAttributes and CodeAttributes
    }

    @Override
    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute)
    {
        lineNumber = lineNumberTableAttribute.getLineNumber(offset);
    }

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        codeAttribute.attributesAccept(clazz, method, this);
    }

}
