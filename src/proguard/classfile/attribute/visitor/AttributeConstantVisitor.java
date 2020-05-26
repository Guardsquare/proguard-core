/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.classfile.attribute.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.Field;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.ConstantValueAttribute;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This AttributeVisitor lets a given ConstantVisitor visit all constants
 * of the constant value attributes it visits.
 *
 * @author Eric Lafortune
 */
public class AttributeConstantVisitor
implements AttributeVisitor
{
    private final ConstantVisitor constantVisitor;


    /**
     * Creates a new InstructionConstantVisitor.
     * @param constantVisitor the ConstantVisitor to which visits will be
     *                        delegated.
     */
    public AttributeConstantVisitor(ConstantVisitor constantVisitor)
    {
        this.constantVisitor = constantVisitor;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitConstantValueAttribute(Clazz clazz, Field field, ConstantValueAttribute constantValueAttribute)
    {
        clazz.constantPoolEntryAccept(constantValueAttribute.u2constantValueIndex,
                                      constantVisitor);
    }
}