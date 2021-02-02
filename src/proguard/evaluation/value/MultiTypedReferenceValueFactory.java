/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.evaluation.value;

import proguard.classfile.Clazz;

/**
 * This class provides a wrapper around {@link TypedReferenceValueFactory}
 * that provides new {@link MultiTypedReferenceValue}s.
 *
 * @author Samuel Hopstock
 */
public class MultiTypedReferenceValueFactory
    extends TypedReferenceValueFactory
{

    private MultiTypedReferenceValue wrap(ReferenceValue base)
    {
        if (base instanceof MultiTypedReferenceValue)
        {
            return (MultiTypedReferenceValue) base;
        }
        else if (base instanceof TypedReferenceValue)
        {
            return new MultiTypedReferenceValue((TypedReferenceValue) base, false);
        }
        throw new IllegalStateException("Can't handle value of type " + base.getClass().getSimpleName());
    }

    @Override
    public ReferenceValue createReferenceValueNull()
    {
        return wrap(super.createReferenceValueNull());
    }

    @Override
    public ReferenceValue createReferenceValue(String type, Clazz referencedClass, boolean mayBeExtension, boolean mayBeNull)
    {
        return wrap(super.createReferenceValue(type, referencedClass, mayBeExtension, mayBeNull));
    }

    @Override
    public ReferenceValue createArrayReferenceValue(String type, Clazz referencedClass, IntegerValue arrayLength)
    {
        return wrap(super.createArrayReferenceValue(type, referencedClass, arrayLength));
    }

    @Override
    public ReferenceValue createArrayReferenceValue(String type, Clazz referencedClass, IntegerValue arrayLength, Value elementValue)
    {
        return wrap(super.createArrayReferenceValue(type, referencedClass, arrayLength, elementValue));
    }
}
