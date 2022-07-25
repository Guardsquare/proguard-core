/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2021 Guardsquare NV
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
package proguard.evaluation.value;

/**
 * This {@link IntegerValue} represents a particular integer value.
 * <p/>
 * This class handles interactions with:
 * - ParticularIntegerValue
 * <p/>
 * It reverses and delegates interactions with:
 * - RangeIntegerValue
 * - IntegerValue (in general)
 * <p/>
 * It notably doesn't handle interactions with:
 * - UnknownInteger
 * - RangeIntegerValue
 * - SpecificInteger (in general)
 *
 * @author Eric Lafortune
 */
public final class ParticularIntegerValue extends SpecificIntegerValue
{
    private final int value;


    /**
     * Creates a new particular integer value.
     */
    public ParticularIntegerValue(int value)
    {
        this.value = value;
    }


    // Implementations for IntegerValue.

    public int value()
    {
        return value;
    }


    // Implementations of unary methods of IntegerValue.

    public IntegerValue negate()
    {
        return new ParticularIntegerValue(-value);
    }

    public IntegerValue convertToByte()
    {
        int byteValue = (byte)value;

        return byteValue == value ?
            this :
            new ParticularIntegerValue(byteValue);
    }

    public IntegerValue convertToCharacter()
    {
        int charValue = (char)value;

        return charValue == value ?
            this :
            new ParticularIntegerValue(charValue);
    }

    public IntegerValue convertToShort()
    {
        int shortValue = (short)value;

        return shortValue == value ?
            this :
            new ParticularIntegerValue(shortValue);
    }

    public LongValue convertToLong()
    {
        return new ParticularLongValue((long)value);
    }

    public FloatValue convertToFloat()
    {
        return new ParticularFloatValue((float)value);
    }

    public DoubleValue convertToDouble()
    {
        return new ParticularDoubleValue((double)value);
    }


    // Implementations of binary methods of IntegerValue.

    public IntegerValue generalize(IntegerValue other)
    {
        return other.generalize(this);
    }

    public IntegerValue add(IntegerValue other)
    {
        return other.add(this);
    }

    public IntegerValue subtract(IntegerValue other)
    {
        return other.subtractFrom(this);
    }

    public IntegerValue subtractFrom(IntegerValue other)
    {
        return other.subtract(this);
    }

    public IntegerValue multiply(IntegerValue other)
    {
        return other.multiply(this);
    }

    public IntegerValue divide(IntegerValue other)
    throws ArithmeticException
    {
        return other.divideOf(this);
    }

    public IntegerValue divideOf(IntegerValue other)
    throws ArithmeticException
    {
        return other.divide(this);
    }

    public IntegerValue remainder(IntegerValue other)
    throws ArithmeticException
    {
        return other.remainderOf(this);
    }

    public IntegerValue remainderOf(IntegerValue other)
    throws ArithmeticException
    {
        return other.remainder(this);
    }

    public IntegerValue shiftLeft(IntegerValue other)
    {
        return other.shiftLeftOf(this);
    }

    public IntegerValue shiftLeftOf(IntegerValue other)
    {
        return other.shiftLeft(this);
    }

    public IntegerValue shiftRight(IntegerValue other)
    {
        return other.shiftRightOf(this);
    }

    public IntegerValue shiftRightOf(IntegerValue other)
    {
        return other.shiftRight(this);
    }

    public IntegerValue unsignedShiftRight(IntegerValue other)
    {
        return other.unsignedShiftRightOf(this);
    }

    public IntegerValue unsignedShiftRightOf(IntegerValue other)
    {
        return other.unsignedShiftRight(this);
    }

    public LongValue shiftLeftOf(LongValue other)
    {
        return other.shiftLeft(this);
    }

    public LongValue shiftRightOf(LongValue other)
    {
        return other.shiftRight(this);
    }

    public LongValue unsignedShiftRightOf(LongValue other)
    {
        return other.unsignedShiftRight(this);
    }

    public IntegerValue and(IntegerValue other)
    {
        return other.and(this);
    }

    public IntegerValue or(IntegerValue other)
    {
        return other.or(this);
    }

    public IntegerValue xor(IntegerValue other)
    {
        return other.xor(this);
    }

    public int equal(IntegerValue other)
    {
        return other.equal(this);
    }

    public int lessThan(IntegerValue other)
    {
        return other.greaterThan(this);
    }

    public int lessThanOrEqual(IntegerValue other)
    {
        return other.greaterThanOrEqual(this);
    }


    // Implementations of binary IntegerValue methods with ParticularIntegerValue
    // arguments.

    public IntegerValue generalize(ParticularIntegerValue other)
    {
        return generalize((SpecificIntegerValue)other);
    }

    public IntegerValue add(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value + other.value);
    }

    public IntegerValue subtract(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value - other.value);
    }

    public IntegerValue subtractFrom(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(other.value - this.value);
    }

    public IntegerValue multiply(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value * other.value);
    }

    public IntegerValue divide(ParticularIntegerValue other)
    throws ArithmeticException
    {
        return new ParticularIntegerValue(this.value / other.value);
    }

    public IntegerValue divideOf(ParticularIntegerValue other)
    throws ArithmeticException
    {
        return new ParticularIntegerValue(other.value / this.value);
    }

    public IntegerValue remainder(ParticularIntegerValue other)
    throws ArithmeticException
    {
        return new ParticularIntegerValue(this.value % other.value);
    }

    public IntegerValue remainderOf(ParticularIntegerValue other)
    throws ArithmeticException
    {
        return new ParticularIntegerValue(other.value % this.value);
    }

    public IntegerValue shiftLeft(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value << other.value);
    }

    public IntegerValue shiftRight(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value >> other.value);
    }

    public IntegerValue unsignedShiftRight(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value >>> other.value);
    }

    public IntegerValue shiftLeftOf(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(other.value << this.value);
    }

    public IntegerValue shiftRightOf(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(other.value >> this.value);
    }

    public IntegerValue unsignedShiftRightOf(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(other.value >>> this.value);
    }

    public LongValue shiftLeftOf(ParticularLongValue other)
    {
        return new ParticularLongValue(other.value() << this.value);
    }

    public LongValue shiftRightOf(ParticularLongValue other)
    {
        return new ParticularLongValue(other.value() >> this.value);
    }

    public LongValue unsignedShiftRightOf(ParticularLongValue other)
    {
        return new ParticularLongValue(other.value() >>> this.value);
    }

    public IntegerValue and(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value & other.value);
    }

    public IntegerValue or(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value | other.value);
    }

    public IntegerValue xor(ParticularIntegerValue other)
    {
        return new ParticularIntegerValue(this.value ^ other.value);
    }

    public int equal(ParticularIntegerValue other)
    {
        return this.value == other.value ? ALWAYS : NEVER;
    }

    public int lessThan(ParticularIntegerValue other)
    {
        return this.value <  other.value ? ALWAYS : NEVER;
    }

    public int lessThanOrEqual(ParticularIntegerValue other)
    {
        return this.value <= other.value ? ALWAYS : NEVER;
    }


    // Implementations of binary methods of RangeIntegerValue.

    public IntegerValue generalize(RangeIntegerValue other)
    {
        return other.generalize(this);
    }

    public IntegerValue add(RangeIntegerValue other)
    {
        return other.add(this);
    }

    public IntegerValue subtract(RangeIntegerValue other)
    {
        return other.subtractFrom(this);
    }

    public IntegerValue subtractFrom(RangeIntegerValue other)
    {
        return other.subtract(this);
    }

    public IntegerValue multiply(RangeIntegerValue other)
    {
        return other.multiply(this);
    }

    public IntegerValue divide(RangeIntegerValue other)
    throws ArithmeticException
    {
        return other.divideOf(this);
    }

    public IntegerValue divideOf(RangeIntegerValue other)
    throws ArithmeticException
    {
        return other.divide(this);
    }

    public IntegerValue remainder(RangeIntegerValue other)
    throws ArithmeticException
    {
        return other.remainderOf(this);
    }

    public IntegerValue remainderOf(RangeIntegerValue other)
    throws ArithmeticException
    {
        return other.remainder(this);
    }

    public IntegerValue shiftLeft(RangeIntegerValue other)
    {
        return other.shiftLeftOf(this);
    }

    public IntegerValue shiftLeftOf(RangeIntegerValue other)
    {
        return other.shiftLeft(this);
    }

    public IntegerValue shiftRight(RangeIntegerValue other)
    {
        return other.shiftRightOf(this);
    }

    public IntegerValue shiftRightOf(RangeIntegerValue other)
    {
        return other.shiftRight(this);
    }

    public IntegerValue unsignedShiftRight(RangeIntegerValue other)
    {
        return other.unsignedShiftRightOf(this);
    }

    public IntegerValue unsignedShiftRightOf(RangeIntegerValue other)
    {
        return other.unsignedShiftRight(this);
    }

    public IntegerValue and(RangeIntegerValue other)
    {
        return other.and(this);
    }

    public IntegerValue or(RangeIntegerValue other)
    {
        return other.or(this);
    }

    public IntegerValue xor(RangeIntegerValue other)
    {
        return other.xor(this);
    }

    public int equal(RangeIntegerValue other)
    {
        return other.equal(this);
    }

    public int lessThan(RangeIntegerValue other)
    {
        return other.greaterThan(this);
    }

    public int lessThanOrEqual(RangeIntegerValue other)
    {
        return other.greaterThanOrEqual(this);
    }


    // Implementations for Value.

    public boolean isParticular()
    {
        return true;
    }


    // Implementations for Object.

    public boolean equals(Object object)
    {
        return super.equals(object) &&
               this.value == ((ParticularIntegerValue)object).value;
    }


    public int hashCode()
    {
        return this.getClass().hashCode() ^
               value;
    }


    public String toString()
    {
        return Integer.toString(value);
    }
}
