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
package proguard.evaluation.value;

import proguard.evaluation.exception.StackInstructionEvaluationException;

/**
 * This abstract class represents a partially evaluated value.
 *
 * @author Eric Lafortune
 */
public abstract class Value
{
    public static final int NEVER  = -1;
    public static final int MAYBE  = 0;
    public static final int ALWAYS = 1;

    public static final int TYPE_UNKNOWN            = -1;
    public static final int TYPE_INTEGER            = 1;
    public static final int TYPE_LONG               = 2;
    public static final int TYPE_FLOAT              = 3;
    public static final int TYPE_DOUBLE             = 4;
    public static final int TYPE_REFERENCE          = 5;
    public static final int TYPE_INSTRUCTION_OFFSET = 6;
    public static final int TYPE_TOP                = 7;


    /**
     * Returns this Value as a Category1Value.
     */
    public Category1Value category1Value()
    {
        // TODO(MJ): what is this? How do we trigger?
        throw new IllegalArgumentException("Value \"" + this.toString() + "\" is not a Category 1 value [" + this.getClass().getName() + "]");
    }

    /**
     * Returns this Value as a Category2Value.
     */
    public Category2Value category2Value()
    {
        // TODO(MJ): what is this? How do we trigger?
        throw new IllegalArgumentException("Value \"" + this.toString() + "\" is not a Category 2 value [" + this.getClass().getName() + "]");
    }


    /**
     * Returns this Value as an IntegerValue.
     */
    public IntegerValue integerValue()
    {
        throw new StackInstructionEvaluationException("Instruction expects an int value but found: \"" + this.toString() + "\".",
                "You might want to check how this value got onto the stack or look whether the current instruction is correct. Type should match \""+this.getClass().getName()+"\".");
    }

    /**
     * Returns this Value as a LongValue.
     */
    public LongValue longValue()
    {
        throw new StackInstructionEvaluationException("Instruction expects a long value but found: \"" + this.toString() + "\".",
                "You might want to check how this value got onto the stack or look whether the current instruction is correct. Type should match \""+this.getClass().getName()+"\".");
    }

    /**
     * Returns this Value as a FloatValue.
     */
    public FloatValue floatValue()
    {
        throw new StackInstructionEvaluationException("Instruction expects a float value but found: \"" + this.toString() + "\".",
                "You might want to check how this value got onto the stack or look whether the current instruction is correct. Type should match \""+this.getClass().getName()+"\".");
    }

    /**
     * Returns this Value as a DoubleValue.
     */
    public DoubleValue doubleValue()
    {
        throw new StackInstructionEvaluationException("Instruction expects a double value but found: \"" + this.toString() + "\".",
                "You might want to check how this value got onto the stack or look whether the current instruction is correct. Type should match \""+this.getClass().getName()+"\".");
    }

    /**
     * Returns this Value as a ReferenceValue.
     */
    public ReferenceValue referenceValue()
    {
        throw new StackInstructionEvaluationException("Instruction expects a reference value but found: \"" + this.toString() + "\".",
                "You might want to check how this value got onto the stack or look whether the current instruction is correct. Type should match \""+this.getClass().getName()+"\".");
    }

    /**
     * Returns this Value as an InstructionOffsetValue.
     */
    public InstructionOffsetValue instructionOffsetValue()
    {
        throw new StackInstructionEvaluationException("Instruction expects an offset value value but found: \"" + this.toString() + "\".",
                "You might want to check how this value got onto the stack or look whether the current instruction is correct. Type should match \""+this.getClass().getName()+"\".");
    }


    /**
     * Returns whether this Value represents a single specific (but possibly
     * unknown) value.
     */
    public boolean isSpecific()
    {
        return false;
    }


    /**
     * Returns whether this Value represents a single particular (known)
     * value.
     */
    public boolean isParticular()
    {
        return false;
    }


    /**
     * Returns the generalization of this Value and the given other Value.
     */
    public abstract Value generalize(Value other);


    /**
     * Returns whether the computational type of this Value is a category 2 type.
     * This means that it takes up the space of two category 1 types on the
     * stack, for instance.
     */
    public abstract boolean isCategory2();


    /**
     * Returns the computational type of this Value.
     * @return <code>TYPE_INTEGER</code>,
     *         <code>TYPE_LONG</code>,
     *         <code>TYPE_FLOAT</code>,
     *         <code>TYPE_DOUBLE</code>,
     *         <code>TYPE_REFERENCE</code>, or
     *         <code>TYPE_INSTRUCTION_OFFSET</code>.
     */
    public abstract int computationalType();


    /**
     * Returns the internal type of this Value.
     * @return <code>TypeConstants.BOOLEAN</code>,
     *         <code>TypeConstants.BYTE</code>,
     *         <code>TypeConstants.CHAR</code>,
     *         <code>TypeConstants.SHORT</code>,
     *         <code>TypeConstants.INT</code>,
     *         <code>TypeConstants.LONG</code>,
     *         <code>TypeConstants.FLOAT</code>,
     *         <code>TypeConstants.DOUBLE</code>,
     *         <code>TypeConstants.CLASS_START ... TypeConstants.CLASS_END</code>, or
     *         an array type containing any of these types (always as String).
     */
    public abstract String internalType();

    /**
     * Returns a deep copy of the value if it mutable,
     * returns the value itself otherwise.
     */
    public Value copyIfMutable()
    {
        return this;
    }
}
