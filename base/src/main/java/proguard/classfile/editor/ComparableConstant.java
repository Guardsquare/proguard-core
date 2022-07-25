/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.util.ArrayUtil;


/**
 * This class is a {@link Comparable} wrapper of {@link Constant}
 * instances. It can store an index, in order to identify the constant pool
 * entry after it has been sorted. The comparison is primarily based on the
 * types of the constant pool entries, and secondarily on the contents of
 * the constant pool entries.
 *
 * @author Eric Lafortune
 */
class      ComparableConstant
implements Comparable, ConstantVisitor
{
    private static final int[] PRIORITIES = new int[100];
    static
    {
        PRIORITIES[Constant.INTEGER]             =  0; // Possibly byte index (ldc).
        PRIORITIES[Constant.FLOAT]               =  1;
        PRIORITIES[Constant.STRING]              =  2;
        PRIORITIES[Constant.CLASS]               =  3;
        PRIORITIES[Constant.LONG]                =  4; // Always wide index (ldc2_w).
        PRIORITIES[Constant.DOUBLE]              =  5; // Always wide index (ldc2_w).
        PRIORITIES[Constant.FIELDREF]            =  6; // Always wide index (getfield,...).
        PRIORITIES[Constant.METHODREF]           =  7; // Always wide index (invokespecial,...).
        PRIORITIES[Constant.INTERFACE_METHODREF] =  8; // Always wide index (invokeinterface).
        PRIORITIES[Constant.DYNAMIC]             =  9; // Always wide index (invokedynamic).
        PRIORITIES[Constant.INVOKE_DYNAMIC]      = 10; // Always wide index (invokedynamic).
        PRIORITIES[Constant.METHOD_HANDLE]       = 11;
        PRIORITIES[Constant.NAME_AND_TYPE]       = 12;
        PRIORITIES[Constant.METHOD_TYPE]         = 13;
        PRIORITIES[Constant.MODULE]              = 14;
        PRIORITIES[Constant.PACKAGE]             = 15;
        PRIORITIES[Constant.UTF8]                = 16;
        PRIORITIES[Constant.PRIMITIVE_ARRAY]     = 17;
    }

    private final Clazz    clazz;
    private final int      thisIndex;
    private final Constant thisConstant;

    private Constant otherConstant;
    private int      result;


    public ComparableConstant(Clazz clazz, int index, Constant constant)
    {
        this.clazz        = clazz;
        this.thisIndex    = index;
        this.thisConstant = constant;
    }


    public int getIndex()
    {
        return thisIndex;
    }


    public Constant getConstant()
    {
        return thisConstant;
    }


    // Implementations for Comparable.

    public int compareTo(Object other)
    {
        ComparableConstant otherComparableConstant = (ComparableConstant)other;

        otherConstant = otherComparableConstant.thisConstant;

        // Compare based on the original indices, if the actual constant pool
        // entries are the same.
        if (thisConstant == otherConstant)
        {
            int otherIndex = otherComparableConstant.thisIndex;

            return Integer.compare(thisIndex, otherIndex);
        }

        // Compare based on the tags, if they are different.
        int thisTag  = thisConstant.getTag();
        int otherTag = otherConstant.getTag();

        if (thisTag != otherTag)
        {
            return Integer.compare(PRIORITIES[thisTag], PRIORITIES[otherTag]);
        }

        // Otherwise compare based on the contents of the Constant objects.
        thisConstant.accept(clazz, this);

        return result;
    }


    // Implementations for ConstantVisitor.

    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        result = Integer.compare(integerConstant.getValue(),
                                 ((IntegerConstant)otherConstant).getValue());
    }

    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        result = Long.compare(longConstant.getValue(),
                              ((LongConstant)otherConstant).getValue());
    }

    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        result = Float.compare(floatConstant.getValue(),
                               ((FloatConstant)otherConstant).getValue());
    }

    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        result = Double.compare(doubleConstant.getValue(),
                                ((DoubleConstant)otherConstant).getValue());
    }

    public void visitPrimitiveArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant)
    {
        PrimitiveArrayConstant otherPrimitiveArrayConstant =
            (PrimitiveArrayConstant)otherConstant;

        char primitiveType      = primitiveArrayConstant.getPrimitiveType();
        char otherPrimitiveType = otherPrimitiveArrayConstant.getPrimitiveType();

        if (primitiveType != otherPrimitiveType)
        {
            result = Integer.compare(primitiveType, otherPrimitiveType);
        }
        else
        {
            Object values      = primitiveArrayConstant.getValues();
            Object otherValues = otherPrimitiveArrayConstant.getValues();

            result =
                values instanceof boolean[] ? ArrayUtil.compare((boolean[])values, ((boolean[])values).length, (boolean[])otherValues, ((boolean[])otherValues).length) :
                values instanceof byte[]    ? ArrayUtil.compare((byte[])   values, ((byte[])   values).length, (byte[])   otherValues, ((byte[])   otherValues).length) :
                values instanceof char[]    ? ArrayUtil.compare((char[])   values, ((char[])   values).length, (char[])   otherValues, ((char[])   otherValues).length) :
                values instanceof short[]   ? ArrayUtil.compare((short[])  values, ((short[])  values).length, (short[])  otherValues, ((short[])  otherValues).length) :
                values instanceof int[]     ? ArrayUtil.compare((int[])    values, ((int[])    values).length, (int[])    otherValues, ((int[])    otherValues).length) :
                values instanceof float[]   ? ArrayUtil.compare((float[])  values, ((float[])  values).length, (float[])  otherValues, ((float[])  otherValues).length) :
                values instanceof long[]    ? ArrayUtil.compare((long[])   values, ((long[])   values).length, (long[])   otherValues, ((long[])   otherValues).length) :
              /*values instanceof double[] */ ArrayUtil.compare((double[]) values, ((double[]) values).length, (double[]) otherValues, ((double[]) otherValues).length);
        }
    }

    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        result = stringConstant.getString(clazz).compareTo(((StringConstant)otherConstant).getString(clazz));
    }

    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        result = utf8Constant.getString().compareTo(((Utf8Constant)otherConstant).getString());
    }

    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        DynamicConstant otherDynamicConstant = (DynamicConstant)otherConstant;

        int index      = dynamicConstant.getBootstrapMethodAttributeIndex();
        int otherIndex = otherDynamicConstant.getBootstrapMethodAttributeIndex();

        result = index < otherIndex ? -1 :
                 index > otherIndex ?  1 :
                     compare(dynamicConstant.getName(clazz),
                             dynamicConstant.getType(clazz),
                             otherDynamicConstant.getName(clazz),
                             otherDynamicConstant.getType(clazz));
    }

    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        InvokeDynamicConstant otherInvokeDynamicConstant = (InvokeDynamicConstant)otherConstant;

        int index      = invokeDynamicConstant.getBootstrapMethodAttributeIndex();
        int otherIndex = otherInvokeDynamicConstant.getBootstrapMethodAttributeIndex();

        result = index < otherIndex ? -1 :
                 index > otherIndex ?  1 :
                     compare(invokeDynamicConstant.getName(clazz),
                             invokeDynamicConstant.getType(clazz),
                             otherInvokeDynamicConstant.getName(clazz),
                             otherInvokeDynamicConstant.getType(clazz));
    }

    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        MethodHandleConstant otherMethodHandleConstant = (MethodHandleConstant)otherConstant;

        int kind      = methodHandleConstant.getReferenceKind();
        int otherKind = otherMethodHandleConstant.getReferenceKind();

        result = kind < otherKind ? -1 :
                 kind > otherKind ?  1 :
                     compare(methodHandleConstant.getClassName(clazz),
                             methodHandleConstant.getName(clazz),
                             methodHandleConstant.getType(clazz),
                             otherMethodHandleConstant.getClassName(clazz),
                             otherMethodHandleConstant.getName(clazz),
                             otherMethodHandleConstant.getType(clazz));
    }

    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        RefConstant otherRefConstant = (RefConstant)otherConstant;
        result = compare(refConstant.getClassName(clazz),
                         refConstant.getName(clazz),
                         refConstant.getType(clazz),
                         otherRefConstant.getClassName(clazz),
                         otherRefConstant.getName(clazz),
                         otherRefConstant.getType(clazz));
    }

    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        result = classConstant.getName(clazz).compareTo(((ClassConstant)otherConstant).getName(clazz));
    }

    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant MethodTypeConstant)
    {
        MethodTypeConstant otherMethodTypeConstant = (MethodTypeConstant)otherConstant;
        result = MethodTypeConstant.getType(clazz)
                 .compareTo
                 (otherMethodTypeConstant.getType(clazz));
    }

    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        NameAndTypeConstant otherNameAndTypeConstant = (NameAndTypeConstant)otherConstant;
        result = compare(nameAndTypeConstant.getName(clazz),
                         nameAndTypeConstant.getType(clazz),
                         otherNameAndTypeConstant.getName(clazz),
                         otherNameAndTypeConstant.getType(clazz));
    }


    public void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        result = moduleConstant.getName(clazz).compareTo(((ModuleConstant)otherConstant).getName(clazz));
    }


    public void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        result = packageConstant.getName(clazz).compareTo(((PackageConstant)otherConstant).getName(clazz));
    }

    // Implementations for Object.

    public boolean equals(Object other)
    {
        return other != null &&
               this.getClass().equals(other.getClass()) &&
               this.getConstant().getClass().equals(((ComparableConstant)other).getConstant().getClass()) &&
               this.compareTo(other) == 0;
    }


    public int hashCode()
    {
        return this.getClass().hashCode();
    }


    // Small utility methods.

    /**
     * Compares the given two pairs of strings.
     */
    private int compare(String string1a, String string1b,
                        String string2a, String string2b)
    {
        int comparison;
        return
            (comparison = string1a.compareTo(string2a)) != 0 ? comparison :
                          string1b.compareTo(string2b);
    }


    /**
     * Compares the given two triplets of strings.
     */
    private int compare(String string1a, String string1b, String string1c,
                        String string2a, String string2b, String string2c)
    {
        int comparison;
        return
            (comparison = string1a.compareTo(string2a)) != 0 ? comparison :
            (comparison = string1b.compareTo(string2b)) != 0 ? comparison :
                          string1c.compareTo(string2c);
    }
}
