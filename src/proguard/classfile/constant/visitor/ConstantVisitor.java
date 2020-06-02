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
 * This interface specifies the methods for a visitor of {@link Constant}
 * instances.
 *
 * @author Eric Lafortune
 */
public interface ConstantVisitor
{
    /**
     * Visits any Constant instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyConstant(Clazz clazz, Constant constant)
    {
        throw new UnsupportedOperationException(this.getClass().getName()+" does not support "+constant.getClass().getName());
    }


    default void visitIntegerConstant(Clazz clazz, IntegerConstant integerConstant)
    {
        visitAnyConstant(clazz, integerConstant);
    }


    default void visitLongConstant(Clazz clazz, LongConstant longConstant)
    {
        visitAnyConstant(clazz, longConstant);
    }


    default void visitFloatConstant(Clazz clazz, FloatConstant floatConstant)
    {
        visitAnyConstant(clazz, floatConstant);
    }


    default void visitDoubleConstant(Clazz clazz, DoubleConstant doubleConstant)
    {
        visitAnyConstant(clazz, doubleConstant);
    }


    default void visitPrimitiveArrayConstant(Clazz clazz, PrimitiveArrayConstant primitiveArrayConstant)
    {
        visitAnyConstant(clazz, primitiveArrayConstant);
    }


    default void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        visitAnyConstant(clazz, stringConstant);
    }


    default void visitUtf8Constant(Clazz clazz, Utf8Constant utf8Constant)
    {
        visitAnyConstant(clazz, utf8Constant);
    }


    default void visitDynamicConstant(Clazz clazz, DynamicConstant dynamicConstant)
    {
        visitAnyConstant(clazz, dynamicConstant);
    }


    default void visitInvokeDynamicConstant(Clazz clazz, InvokeDynamicConstant invokeDynamicConstant)
    {
        visitAnyConstant(clazz, invokeDynamicConstant);
    }


    default void visitMethodHandleConstant(Clazz clazz, MethodHandleConstant methodHandleConstant)
    {
        visitAnyConstant(clazz, methodHandleConstant);
    }

    default void visitModuleConstant(Clazz clazz, ModuleConstant moduleConstant)
    {
        visitAnyConstant(clazz, moduleConstant);
    }

    default void visitPackageConstant(Clazz clazz, PackageConstant packageConstant)
    {
        visitAnyConstant(clazz, packageConstant);
    }



    /**
     * Visits any RefConstant instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        visitAnyConstant(clazz, refConstant);
    }


    default void visitFieldrefConstant(Clazz clazz, FieldrefConstant fieldrefConstant)
    {
        visitAnyRefConstant(clazz, fieldrefConstant);
    }



    /**
     * Visits any RefConstant instance. The more specific default implementations of
     * this interface delegate to this method.
     */
    default void visitAnyMethodrefConstant(Clazz clazz, AnyMethodrefConstant anyMethodrefConstant)
    {
        visitAnyRefConstant(clazz, anyMethodrefConstant);
    }


    default void visitInterfaceMethodrefConstant(Clazz clazz, InterfaceMethodrefConstant interfaceMethodrefConstant)
    {
        visitAnyMethodrefConstant(clazz, interfaceMethodrefConstant);
    }


    default void visitMethodrefConstant(Clazz clazz, MethodrefConstant methodrefConstant)
    {
        visitAnyMethodrefConstant(clazz, methodrefConstant);
    }


    default void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        visitAnyConstant(clazz, classConstant);
    }


    default void visitMethodTypeConstant(Clazz clazz, MethodTypeConstant methodTypeConstant)
    {
        visitAnyConstant(clazz, methodTypeConstant);
    }


    default void visitNameAndTypeConstant(Clazz clazz, NameAndTypeConstant nameAndTypeConstant)
    {
        visitAnyConstant(clazz, nameAndTypeConstant);
    }
}
