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
package proguard.classfile.constant.visitor;

import proguard.classfile.Clazz;
import proguard.classfile.constant.*;

/**
 * This {@link ConstantVisitor} collects the constants that it visits in a
 * readable form, in a given string builder.
 *
 * @author Eric Lafortune
 */
public class ConstantStringBuilder implements ConstantVisitor
{
    private final StringBuilder stringBuilder;


    /**
     * Creates a new ConstantStringBuilder.
     * @param stringBuilder the string builder in which descriptions can be
     *                      collected.
     */
    public ConstantStringBuilder(StringBuilder stringBuilder)
    {
        this.stringBuilder = stringBuilder;
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant)
    {
        stringBuilder.append("Unknown");
    }


    public void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        stringBuilder.append("Integer("+integerConstant.u4value+")");
    }


    public void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        stringBuilder.append("Long("+longConstant.u8value+")");
    }


    public void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        stringBuilder.append("Float("+floatConstant.f4value+")");
    }


    public void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        stringBuilder.append("Double("+doubleConstant.f8value+")");
    }


    public void visitPrimitiveArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant)
    {
        stringBuilder.append("PrimitiveArray("+primitiveArrayConstant.getPrimitiveType()+"["+primitiveArrayConstant.getLength()+"])");
    }


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        stringBuilder.append("String(\""+stringConstant.getString(clazz)+"\")");
    }


    public void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        stringBuilder.append("Utf8(\""+utf8Constant.getString()+"\")");
    }


    public void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        stringBuilder.append("Dynamic(");
        clazz.constantPoolEntryAccept(dynamicConstant.u2nameAndTypeIndex, this);
        stringBuilder.append(", #"+dynamicConstant.u2bootstrapMethodAttributeIndex+")");
    }


    public void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        stringBuilder.append("InvokeDynamic(");
        clazz.constantPoolEntryAccept(invokeDynamicConstant.u2nameAndTypeIndex, this);
        stringBuilder.append(", #"+invokeDynamicConstant.u2bootstrapMethodAttributeIndex+")");
    }


    public void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        stringBuilder.append("MethodHandle("+methodHandleConstant.u1referenceKind+", ");
        clazz.constantPoolEntryAccept(methodHandleConstant.u2referenceIndex, this);
        stringBuilder.append(")");
    }


    public void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        stringBuilder.append("Module("+moduleConstant.getName(clazz)+")");
    }


    public void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        stringBuilder.append("Package("+packageConstant.getName(clazz)+")");
    }


    public void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        stringBuilder.append("Fieldref("+fieldrefConstant.getClassName(clazz)+"."+fieldrefConstant.getName(clazz)+" "+fieldrefConstant.getType(clazz)+")");
    }


    public void visitInterfaceMethodrefConstant(Clazz clazz, InterfaceMethodrefConstant interfaceMethodrefConstant)
    {
        stringBuilder.append("InterfaceMethodref("+interfaceMethodrefConstant.getClassName(clazz)+"."+interfaceMethodrefConstant.getName(clazz)+interfaceMethodrefConstant.getType(clazz)+")");
    }


    public void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant)
    {
        stringBuilder.append("Methodref("+methodrefConstant.getClassName(clazz)+"."+methodrefConstant.getName(clazz)+methodrefConstant.getType(clazz)+")");
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        stringBuilder.append("Class("+classConstant.getName(clazz)+")");
    }


    public void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        stringBuilder.append("MethodType("+methodTypeConstant.getType(clazz)+")");
    }


    public void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        stringBuilder.append("NameAndType("+nameAndTypeConstant.getName(clazz)+", "+nameAndTypeConstant.getType(clazz)+")");
    }
}
