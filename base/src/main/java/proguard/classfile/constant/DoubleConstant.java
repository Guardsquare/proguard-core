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
package proguard.classfile.constant;

import proguard.classfile.*;
import proguard.classfile.constant.visitor.ConstantVisitor;

/**
 * This {@link Constant} represents a double constant in the constant pool.
 *
 * @author Eric Lafortune
 */
public class DoubleConstant extends Constant
{
    public double f8value;


    /**
     * Creates an uninitialized DoubleConstant.
     */
    public DoubleConstant()
    {
    }


    /**
     * Creates a new DoubleConstant with the given double value.
     */
    public DoubleConstant(double value)
    {
        f8value = value;
    }


    /**
     * Returns the double value of this DoubleConstant.
     */
    public double getValue()
    {
        return f8value;
    }


    /**
     * Sets the double value of this DoubleConstant.
     */
    public void setValue(double value)
    {
        f8value = value;
    }


    // Implementations for Constant.

    @Override
    public int getTag()
    {
        return Constant.DOUBLE;
    }

    @Override
    public boolean isCategory2()
    {
        return true;
    }

    @Override
    public void accept(Clazz clazz, ConstantVisitor constantVisitor)
    {
        constantVisitor.visitDoubleConstant(clazz, this);
    }


    // Implementations for Object.

    @Override
    public boolean equals(Object object)
    {
        if (object == null || !this.getClass().equals(object.getClass()))
        {
            return false;
        }

        if (this == object)
        {
            return true;
        }

        DoubleConstant other = (DoubleConstant)object;

        return
            this.f8value == other.f8value;
    }

    @Override
    public int hashCode()
    {
        long bits = Double.doubleToLongBits(f8value);
        return
            Constant.DOUBLE      ^
            (int)(bits >> 32) ^
            (int)bits;
    }

    @Override
    public String toString()
    {
        return "Double(" + f8value + ")";
    }
}
