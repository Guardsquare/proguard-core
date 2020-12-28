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
 * This {@link Constant} represents a long constant in the constant pool.
 *
 * @author Eric Lafortune
 */
public class LongConstant extends Constant
{
    public long u8value;


    /**
     * Creates an uninitialized LongConstant.
     */
    public LongConstant()
    {
    }


    /**
     * Creates a new LongConstant with the given long value.
     */
    public LongConstant(long value)
    {
        u8value = value;
    }


    /**
     * Returns the long value of this LongConstant.
     */
    public long getValue()
    {
        return u8value;
    }


    /**
     * Sets the long value of this LongConstant.
     */
    public void setValue(long value)
    {
        u8value = value;
    }


    // Implementations for Constant.

    @Override
    public int getTag()
    {
        return Constant.LONG;
    }

    @Override
    public boolean isCategory2()
    {
        return true;
    }

    @Override
    public void accept(Clazz clazz, ConstantVisitor constantVisitor)
    {
        constantVisitor.visitLongConstant(clazz, this);
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

        LongConstant other = (LongConstant)object;

        return
            this.u8value == other.u8value;
    }

    @Override
    public int hashCode()
    {
        return
            Constant.LONG        ^
            (int)(u8value >> 32) ^
            (int)u8value;
    }

    @Override
    public String toString()
    {
        return "Long(" + u8value + ")";
    }
}
