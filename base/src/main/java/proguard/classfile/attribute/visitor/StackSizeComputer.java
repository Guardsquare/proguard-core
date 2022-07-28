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
package proguard.classfile.attribute.visitor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.editor.ClassEstimates;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.visitor.ClassPrinter;
import proguard.util.ArrayUtil;

/**
 * This {@link AttributeVisitor} computes the stack sizes at all instruction offsets
 * of the code attributes that it visits.
 *
 * @author Eric Lafortune
 */
public class StackSizeComputer
implements   AttributeVisitor,
             InstructionVisitor,
             ExceptionInfoVisitor
{
    //*
    private static final boolean DEBUG = false;
    /*/
    private static       boolean DEBUG = System.getProperty("ssc") != null;
    //*/

    private static final Logger logger = LogManager.getLogger(StackSizeComputer.class);

    private boolean[] evaluated        = new boolean[ClassEstimates.TYPICAL_CODE_LENGTH];
    private int[]     stackSizesBefore = new int[ClassEstimates.TYPICAL_CODE_LENGTH];
    private int[]     stackSizesAfter  = new int[ClassEstimates.TYPICAL_CODE_LENGTH];

    private boolean exitInstructionBlock;

    private int stackSize;
    private int maxStackSize;


    /**
     * Returns whether the instruction at the given offset is reachable in the
     * most recently visited code attribute.
     */
    public boolean isReachable(int instructionOffset)
    {
        return evaluated[instructionOffset];
    }


    /**
     * Returns the stack size before the given instruction offset of the most
     * recently visited code attribute.
     */
    public int getStackSizeBefore(int instructionOffset)
    {
        if (!evaluated[instructionOffset])
        {
            throw new IllegalArgumentException("Unknown stack size before unreachable instruction offset ["+instructionOffset+"]");
        }

        return stackSizesBefore[instructionOffset];
    }


    /**
     * Returns the stack size after the given instruction offset of the most
     * recently visited code attribute.
     */
    public int getStackSizeAfter(int instructionOffset)
    {
        if (!evaluated[instructionOffset])
        {
            throw new IllegalArgumentException("Unknown stack size after unreachable instruction offset ["+instructionOffset+"]");
        }

        return stackSizesAfter[instructionOffset];
    }


    /**
     * Returns the maximum stack size of the most recently visited code attribute.
     */
    public int getMaxStackSize()
    {
        return maxStackSize;
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
//        DEBUG =
//            clazz.getName().equals("abc/Def") &&
//            method.getName(clazz).equals("abc");

        // TODO: Remove this when the code has stabilized.
        // Catch any unexpected exceptions from the actual visiting method.
        try
        {
            // Process the code.
            visitCodeAttribute0(clazz, method, codeAttribute);
        }
        catch (RuntimeException ex)
        {
            logger.error("Unexpected error while computing stack sizes:");
            logger.error("  Class       = [{}]", clazz.getName());
            logger.error("  Method      = [{}{}]", method.getName(clazz), method.getDescriptor(clazz));
            logger.error("  Exception   = [{}] ({})", ex.getClass().getName(), ex.getMessage());

            if (DEBUG)
            {
                method.accept(clazz, new ClassPrinter());
            }

            throw ex;
        }
    }


    public void visitCodeAttribute0(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        if (DEBUG)
        {
            System.out.println("StackSizeComputer: "+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz));
        }

        int codeLength = codeAttribute.u4codeLength;

        // Make sure the global arrays are sufficiently large.
        evaluated        = ArrayUtil.ensureArraySize(evaluated,        codeLength, false);
        stackSizesBefore = ArrayUtil.ensureArraySize(stackSizesBefore, codeLength, 0);
        stackSizesAfter  = ArrayUtil.ensureArraySize(stackSizesAfter,  codeLength, 0);

        // The initial stack is always empty.
        stackSize    = 0;
        maxStackSize = 0;

        // Evaluate the instruction block starting at the entry point of the method.
        evaluateInstructionBlock(clazz, method, codeAttribute, 0);

        // Evaluate the exception handlers.
        codeAttribute.exceptionsAccept(clazz, method, this);
    }


    // Implementations for InstructionVisitor.

    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        byte opcode = simpleInstruction.opcode;

        // Some simple instructions exit from the current instruction block.
        exitInstructionBlock =
            opcode == Instruction.OP_IRETURN ||
            opcode == Instruction.OP_LRETURN ||
            opcode == Instruction.OP_FRETURN ||
            opcode == Instruction.OP_DRETURN ||
            opcode == Instruction.OP_ARETURN ||
            opcode == Instruction.OP_RETURN  ||
            opcode == Instruction.OP_ATHROW;
    }

    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        // Constant pool instructions never end the current instruction block.
        exitInstructionBlock = false;
    }

    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        byte opcode = variableInstruction.opcode;

        // The ret instruction end the current instruction block.
        exitInstructionBlock =
            opcode == Instruction.OP_RET;
    }

    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        byte opcode = branchInstruction.opcode;

        // Evaluate the target instruction blocks.
        evaluateInstructionBlock(clazz,
                                 method,
                                 codeAttribute,
                                 offset +
                                 branchInstruction.branchOffset);

        // Evaluate the instructions after a subroutine branch.
        if (opcode == Instruction.OP_JSR ||
            opcode == Instruction.OP_JSR_W)
        {
            // We assume subroutine calls (jsr and jsr_w instructions) don't
            // change the stack, other than popping the return value.
            stackSize -= 1;

            evaluateInstructionBlock(clazz,
                                     method,
                                     codeAttribute,
                                     offset + branchInstruction.length(offset));
        }

        // Some branch instructions always end the current instruction block.
        exitInstructionBlock =
            opcode == Instruction.OP_GOTO   ||
            opcode == Instruction.OP_GOTO_W ||
            opcode == Instruction.OP_JSR    ||
            opcode == Instruction.OP_JSR_W;
    }


    public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
    {
        // Evaluate the target instruction blocks.

        // Loop over all jump offsets.
        int[] jumpOffsets = switchInstruction.jumpOffsets;

        for (int index = 0; index < jumpOffsets.length; index++)
        {
            // Evaluate the jump instruction block.
            evaluateInstructionBlock(clazz,
                                     method,
                                     codeAttribute,
                                     offset + jumpOffsets[index]);
        }

        // Also evaluate the default instruction block.
        evaluateInstructionBlock(clazz,
                                 method,
                                 codeAttribute,
                                 offset + switchInstruction.defaultOffset);

        // The switch instruction always ends the current instruction block.
        exitInstructionBlock = true;
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        if (DEBUG)
        {
            System.out.println("Exception:");
        }

        // The stack size when entering the exception handler is always 1.
        stackSize = 1;

        // Evaluate the instruction block starting at the entry point of the
        // exception handler.
        evaluateInstructionBlock(clazz,
                                 method,
                                 codeAttribute,
                                 exceptionInfo.u2handlerPC);
    }


    // Small utility methods.

    /**
     * Evaluates a block of instructions that hasn't been handled before,
     * starting at the given offset and ending at a branch instruction, a return
     * instruction, or a throw instruction. Branch instructions are handled
     * recursively.
     */
    private void evaluateInstructionBlock(Clazz         clazz,
                                          Method        method,
                                          CodeAttribute codeAttribute,
                                          int           instructionOffset)
    {
        if (DEBUG)
        {
            if (evaluated[instructionOffset])
            {
                System.out.println("-- (instruction block at "+instructionOffset+" already evaluated)");
            }
            else
            {
                System.out.println("-- instruction block:");
            }
        }

        // Remember the initial stack size.
        int initialStackSize = stackSize;

        // Remember the maximum stack size.
        if (maxStackSize < stackSize)
        {
            maxStackSize = stackSize;
        }

        // Evaluate any instructions that haven't been evaluated before.
        while (!evaluated[instructionOffset])
        {
            // Mark the instruction as evaluated.
            evaluated[instructionOffset]        = true;
            stackSizesBefore[instructionOffset] = stackSize;

            Instruction instruction = InstructionFactory.create(codeAttribute.code,
                                                                instructionOffset);

            if (DEBUG)
            {
                int stackPushCount = instruction.stackPushCount(clazz);
                int stackPopCount  = instruction.stackPopCount(clazz);
                System.out.println("["+instructionOffset+"]: "+
                                   stackSize+" - "+
                                   stackPopCount+" + "+
                                   stackPushCount+" = "+
                                   (stackSize+stackPushCount-stackPopCount)+": "+
                                   instruction.toString(clazz, instructionOffset));
            }

            // Compute the instruction's effect on the stack size.
            stackSize -= instruction.stackPopCount(clazz);

            if (stackSize < 0)
            {
                throw new IllegalArgumentException("Stack size becomes negative after instruction "+
                                                   instruction.toString(clazz, instructionOffset)+" in ["+
                                                   clazz.getName()+"."+
                                                   method.getName(clazz)+
                                                   method.getDescriptor(clazz)+"]");
            }

            stackSize += instruction.stackPushCount(clazz);
            stackSizesAfter[instructionOffset] = stackSize;

            // Remember the maximum stack size.
            if (maxStackSize < stackSize)
            {
                maxStackSize = stackSize;
            }

            // Remember the next instruction offset.
            int nextInstructionOffset = instructionOffset +
                                        instruction.length(instructionOffset);

            // Visit the instruction, in order to handle branches.
            instruction.accept(clazz, method, codeAttribute, instructionOffset, this);

            // Stop evaluating after a branch.
            if (exitInstructionBlock)
            {
                break;
            }

            // Continue with the next instruction.
            instructionOffset = nextInstructionOffset;

            if (DEBUG)
            {
                if (evaluated[instructionOffset])
                {
                    System.out.println("-- (instruction at "+instructionOffset+" already evaluated)");
                }
            }
        }

        // Restore the stack size for possible subsequent instruction blocks.
        this.stackSize = initialStackSize;
    }
}
