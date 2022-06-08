package proguard.classfile.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;

public class CodeSizeCounter
implements AttributeVisitor
{
    private int totalCodeSize = 0;

    // Implementations for AttributeVisitor

    @Override
    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

    @Override
    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        totalCodeSize += codeAttribute.u4codeLength;
    }

    public int getCount()
    {
        return totalCodeSize;
    }
}
