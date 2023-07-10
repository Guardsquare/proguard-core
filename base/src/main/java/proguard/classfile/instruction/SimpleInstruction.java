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
package proguard.classfile.instruction;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.visitor.InstructionVisitor;

import java.util.Objects;

/**
 * This {@link Instruction} represents a simple instruction without variable arguments
 * or constant pool references.
 *
 * @author Eric Lafortune
 */
public class SimpleInstruction extends Instruction
{
    public int constant;


    /**
     * Creates an uninitialized SimpleInstruction.
     */
    public SimpleInstruction() {}


    /**
     * Creates a new SimpleInstruction with the given opcode.
     * Automatically extracts any embedded constant.
     */
    public SimpleInstruction(byte opcode)
    {
        this.opcode   = opcode;
        this.constant = embeddedConstant(opcode);
    }


    /**
     * Creates a new SimpleInstruction with the given opcode and constant.
     * Automatically adapts the opcode if it has an embedded constant and
     * the constant lies within the supported range. If the constant doesn't
     * lie within the supported range, it must be adapted before the
     * instruction can be written out.
     */
    public SimpleInstruction(byte opcode, int constant)
    {
        this.opcode   = embeddingOpcode(opcode, constant);
        this.constant = constant;
    }


    /**
     * Copies the given instruction into this instruction.
     * @param simpleInstruction the instruction to be copied.
     * @return this instruction.
     */
    public SimpleInstruction copy(SimpleInstruction simpleInstruction)
    {
        this.opcode   = simpleInstruction.opcode;
        this.constant = simpleInstruction.constant;

        return this;
    }


    // Small utility methods.

    /**
     * Returns the correct variant of the given opcode that embeds the given
     * constant, if it embeds any.
     */
    private static byte embeddingOpcode(byte opcode, int constant)
    {
        // Replace any _m1, _0, _1, _2, _3,... extension by the right one,
        // based on the constant. Allow out-of-range constants, such as
        // wildcards.
        switch (opcode)
        {
            case Instruction.OP_ICONST_M1:
            case Instruction.OP_ICONST_0:
            case Instruction.OP_ICONST_1:
            case Instruction.OP_ICONST_2:
            case Instruction.OP_ICONST_3:
            case Instruction.OP_ICONST_4:
            case Instruction.OP_ICONST_5:
                return constant >= -1  &&
                       constant <= 5 ?
                    (byte)(Instruction.OP_ICONST_0 + constant) :
                    opcode;

            case Instruction.OP_LCONST_0:
            case Instruction.OP_LCONST_1:
                return constant == 0  ||
                       constant == 1 ?
                    (byte)(Instruction.OP_LCONST_0 + constant) :
                    opcode;

            case Instruction.OP_FCONST_0:
            case Instruction.OP_FCONST_1:
            case Instruction.OP_FCONST_2:
                return constant >= 0  &&
                       constant <= 2 ?
                    (byte)(Instruction.OP_FCONST_0 + constant) :
                    opcode;

            case Instruction.OP_DCONST_0:
            case Instruction.OP_DCONST_1:
                return constant == 0  ||
                       constant == 1 ?
                    (byte)(Instruction.OP_DCONST_0 + constant) :
                    opcode;

            default:
                return opcode;
        }
    }


    /**
     * Returns the embedded constant of the given opcode, or 0 if the opcode
     * doesn't have one.
     */
    private static int embeddedConstant(byte opcode)
    {
        switch (opcode)
        {
            case Instruction.OP_ICONST_M1: return -1;

            case Instruction.OP_ICONST_1:
            case Instruction.OP_LCONST_1:
            case Instruction.OP_FCONST_1:
            case Instruction.OP_DCONST_1: return 1;

            case Instruction.OP_ICONST_2:
            case Instruction.OP_FCONST_2: return 2;

            case Instruction.OP_ICONST_3: return 3;

            case Instruction.OP_ICONST_4: return 4;

            case Instruction.OP_ICONST_5: return 5;

            default: return 0;
        }
    }


    // Implementations for Instruction.

    public byte canonicalOpcode()
    {
        // Replace any _1, _2, _3,... extension by _0.
        switch (opcode)
        {
            case Instruction.OP_ICONST_M1:
            case Instruction.OP_ICONST_0:
            case Instruction.OP_ICONST_1:
            case Instruction.OP_ICONST_2:
            case Instruction.OP_ICONST_3:
            case Instruction.OP_ICONST_4:
            case Instruction.OP_ICONST_5:
            case Instruction.OP_BIPUSH:
            case Instruction.OP_SIPUSH:   return Instruction.OP_ICONST_0;

            case Instruction.OP_LCONST_0:
            case Instruction.OP_LCONST_1: return Instruction.OP_LCONST_0;

            case Instruction.OP_FCONST_0:
            case Instruction.OP_FCONST_1:
            case Instruction.OP_FCONST_2: return Instruction.OP_FCONST_0;

            case Instruction.OP_DCONST_0:
            case Instruction.OP_DCONST_1: return Instruction.OP_DCONST_0;

            default: return opcode;
        }
    }

    public Instruction shrink()
    {
        // Reconstruct the opcode of the shortest instruction, if there are
        // any alternatives.
        switch (opcode)
        {
            case Instruction.OP_ICONST_M1:
            case Instruction.OP_ICONST_0:
            case Instruction.OP_ICONST_1:
            case Instruction.OP_ICONST_2:
            case Instruction.OP_ICONST_3:
            case Instruction.OP_ICONST_4:
            case Instruction.OP_ICONST_5:
            case Instruction.OP_BIPUSH:
            case Instruction.OP_SIPUSH:
                switch (requiredConstantSize())
                {
                    case 0:
                        opcode = (byte)(Instruction.OP_ICONST_0 + constant);
                        break;
                    case 1:
                        opcode = Instruction.OP_BIPUSH;
                        break;
                    case 2:
                        opcode = Instruction.OP_SIPUSH;
                        break;
                }
                break;

            case Instruction.OP_LCONST_0:
            case Instruction.OP_LCONST_1:
                opcode = (byte)(Instruction.OP_LCONST_0 + constant);
                break;

            case Instruction.OP_FCONST_0:
            case Instruction.OP_FCONST_1:
            case Instruction.OP_FCONST_2:
                opcode = (byte)(Instruction.OP_FCONST_0 + constant);
                break;

            case Instruction.OP_DCONST_0:
            case Instruction.OP_DCONST_1:
                opcode = (byte)(Instruction.OP_DCONST_0 + constant);
                break;
        }

        return this;
    }

    protected void readInfo(byte[] code, int offset)
    {
        int constantSize = constantSize();

        // Also initialize embedded constants that are different from 0.
        constant = constantSize == 0 ?
            embeddedConstant(opcode) :
            readSignedValue(code, offset, constantSize);
    }


    protected void writeInfo(byte[] code, int offset)
    {
        int constantSize = constantSize();

        if (requiredConstantSize() > constantSize)
        {
            throw new IllegalArgumentException("Instruction has invalid constant size ("+this.toString(offset)+")");
        }

        writeSignedValue(code, offset, constant, constantSize);
    }


    public int length(int offset)
    {
        return 1 + constantSize();
    }


    public void accept(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, InstructionVisitor instructionVisitor)
    {
        instructionVisitor.visitSimpleInstruction(clazz, method, codeAttribute, offset, this);
    }


    public int stackEntryIndexBefore(Clazz clazz, int stackEntryIndexAfter)
    {
        // Some special cases.
        switch (opcode)
        {
            case Instruction.OP_DUP:     return 0;
            case Instruction.OP_DUP_X1:  return stackEntryIndexAfter >= 2 ? 0                        : stackEntryIndexAfter;
            case Instruction.OP_DUP_X2:  return stackEntryIndexAfter >= 3 ? 0                        : stackEntryIndexAfter;
            case Instruction.OP_DUP2:    return stackEntryIndexAfter >= 2 ? stackEntryIndexAfter - 2 : stackEntryIndexAfter;
            case Instruction.OP_DUP2_X1: return stackEntryIndexAfter >= 3 ? stackEntryIndexAfter - 3 : stackEntryIndexAfter;
            case Instruction.OP_DUP2_X2: return stackEntryIndexAfter >= 4 ? stackEntryIndexAfter - 4 : stackEntryIndexAfter;
            case Instruction.OP_SWAP:    return 1 - stackEntryIndexAfter;
            default:                     return -1;
        }
    }


    // Implementations for Object.

    @Override
    public String toString()
    {
        return getName() +
               (constantSize() > 0 || constant != embeddedConstant(opcode) ? " "+constant : "");
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleInstruction that = (SimpleInstruction)o;
        return opcode == that.opcode &&
               constant == that.constant;
    }


    @Override
    public int hashCode()
    {
        return Objects.hash(opcode, constant);
    }


    // Small utility methods.

    /**
     * Returns the constant size for this instruction.
     */
    private int constantSize()
    {
        return opcode == Instruction.OP_BIPUSH ||
               opcode == Instruction.OP_NEWARRAY ? 1 :
               opcode == Instruction.OP_SIPUSH   ? 2 :
                                                            0;
    }


    /**
     * Computes the required constant size for this instruction.
     */
    private int requiredConstantSize()
    {
        return constant >= -1 && constant <= 5  ? 0 :
               (byte)constant  == constant      ? 1 :
               (short)constant == constant      ? 2 :
                                                  4;
    }
}
