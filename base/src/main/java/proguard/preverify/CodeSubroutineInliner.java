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
package proguard.preverify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.editor.CodeAttributeComposer;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.classfile.util.*;
import proguard.classfile.visitor.*;

/**
 * This AttributeVisitor inlines local subroutines (jsr/ret) in the code
 * attributes that it visits.
 *
 * @author Eric Lafortune
 */
public class CodeSubroutineInliner
implements   AttributeVisitor,
             InstructionVisitor,
             ExceptionInfoVisitor
{
    //*
    private static final boolean DEBUG = false;
    /*/
    private static       boolean DEBUG = System.getProperty("csi") != null;
    //*/

    private final static Logger logger = LogManager.getLogger(CodeSubroutineInliner.class);

    private final BranchTargetFinder    branchTargetFinder    = new BranchTargetFinder();
    private final CodeAttributeComposer codeAttributeComposer = new CodeAttributeComposer(true, true, true);

    private ExceptionInfoVisitor subroutineExceptionInliner = this;
    private int                  clipStart                  = 0;
    private int                  clipEnd                    = Integer.MAX_VALUE;


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
//        DEBUG =
//            clazz.getName().equals("abc/Def") &&
//            method.getName(clazz).equals("abc");
//        CodeAttributeComposer.DEBUG = DEBUG;

        // TODO: Remove this when the subroutine inliner has stabilized.
        // Catch any unexpected exceptions from the actual visiting method.
        try
        {
            // Process the code.
            visitCodeAttribute0(clazz, method, codeAttribute);
        }
        catch (RuntimeException ex)
        {
            logger.error("Unexpected error while inlining subroutines:");
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
        branchTargetFinder.visitCodeAttribute(clazz, method, codeAttribute);

        // Don't bother if there aren't any subroutines anyway.
        if (!branchTargetFinder.containsSubroutines())
        {
            return;
        }

        if (DEBUG)
        {
            System.out.println("SubroutineInliner: processing ["+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz)+"]");
        }

        // Append the body of the code.
        codeAttributeComposer.reset();
        codeAttributeComposer.beginCodeFragment(codeAttribute.u4codeLength);

        // Copy the non-subroutine instructions.
        int offset  = 0;
        while (offset < codeAttribute.u4codeLength)
        {
            Instruction instruction = InstructionFactory.create(codeAttribute.code, offset);
            int instructionLength = instruction.length(offset);

            // Is this a returning subroutine?
            if (branchTargetFinder.isSubroutine(offset) &&
                branchTargetFinder.isSubroutineReturning(offset))
            {
                // Skip the subroutine.
                if (DEBUG)
                {
                    System.out.println("  Skipping original subroutine instruction "+instruction.toString(clazz, offset));
                }

                // Append a label at this offset instead.
                codeAttributeComposer.appendLabel(offset);
            }
            else
            {
                // Copy the instruction, inlining any subroutine call recursively.
                instruction.accept(clazz, method, codeAttribute, offset, this);
            }

            offset += instructionLength;
        }

        // Copy the exceptions. Note that exceptions with empty try blocks
        // are automatically removed.
        codeAttribute.exceptionsAccept(clazz,
                                       method,
                                       subroutineExceptionInliner);

        if (DEBUG)
        {
            System.out.println("  Appending label after code at ["+offset+"]");
        }

        // Append a label just after the code.
        codeAttributeComposer.appendLabel(codeAttribute.u4codeLength);

        // End and update the code attribute.
        codeAttributeComposer.endCodeFragment();
        codeAttributeComposer.visitCodeAttribute(clazz, method, codeAttribute);
    }


    /**
     * Appends the specified subroutine.
     */
    private void inlineSubroutine(Clazz         clazz,
                                  Method        method,
                                  CodeAttribute codeAttribute,
                                  int           subroutineInvocationOffset,
                                  int           subroutineStart)
    {
        int subroutineEnd = branchTargetFinder.subroutineEnd(subroutineStart);

        if (DEBUG)
        {
            System.out.println("  Inlining subroutine ["+subroutineStart+" -> "+subroutineEnd+"] at ["+subroutineInvocationOffset+"]");
        }

        // Don't go inlining exceptions that are already applicable to this
        // subroutine invocation.
        ExceptionInfoVisitor oldSubroutineExceptionInliner = subroutineExceptionInliner;
        int                  oldClipStart                  = clipStart;
        int                  oldClipEnd                    = clipEnd;

        subroutineExceptionInliner =
            new ExceptionExcludedOffsetFilter(subroutineInvocationOffset,
                                              subroutineExceptionInliner);
        clipStart = subroutineStart;
        clipEnd   = subroutineEnd;

        codeAttributeComposer.beginCodeFragment(codeAttribute.u4codeLength);

        // Copy the subroutine instructions, inlining any subroutine calls
        // recursively.
        codeAttribute.instructionsAccept(clazz,
                                         method,
                                         subroutineStart,
                                         subroutineEnd,
                                         this);

        if (DEBUG)
        {
            System.out.println("    Appending label after inlined subroutine at ["+subroutineEnd+"]");
        }

        // Append a label just after the code.
        codeAttributeComposer.appendLabel(subroutineEnd);

        // Inline the subroutine exceptions.
        codeAttribute.exceptionsAccept(clazz,
                                       method,
                                       subroutineStart,
                                       subroutineEnd,
                                       subroutineExceptionInliner);

        // We can again inline exceptions that are applicable to this
        // subroutine invocation.
        subroutineExceptionInliner = oldSubroutineExceptionInliner;
        clipStart                  = oldClipStart;
        clipEnd                    = oldClipEnd;

        codeAttributeComposer.endCodeFragment();
    }


    // Implementations for InstructionVisitor.

    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        if (branchTargetFinder.isSubroutineStart(offset))
        {
            if (DEBUG)
            {
                System.out.println("    Replacing first subroutine instruction "+instruction.toString(clazz, offset)+" by a label");
            }

            // Append a label at this offset instead of saving the subroutine
            // return address.
            codeAttributeComposer.appendLabel(offset);
        }
        else
        {
            // Append the instruction.
            codeAttributeComposer.appendInstruction(offset, instruction);
        }
    }


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        byte opcode = variableInstruction.opcode;
        if (opcode == Instruction.OP_RET)
        {
            // Is the return instruction the last instruction of the subroutine?
            if (branchTargetFinder.subroutineEnd(offset) == offset + variableInstruction.length(offset))
            {
                if (DEBUG)
                {
                    System.out.println("    Replacing subroutine return at ["+offset+"] by a label");
                }

                // Append a label at this offset instead of the subroutine return.
                codeAttributeComposer.appendLabel(offset);
            }
            else
            {
                if (DEBUG)
                {
                    System.out.println("    Replacing subroutine return at ["+offset+"] by a simple branch");
                }

                // Replace the instruction by a branch.
                Instruction replacementInstruction =
                    new BranchInstruction(Instruction.OP_GOTO,
                                          branchTargetFinder.subroutineEnd(offset) - offset);

                codeAttributeComposer.appendInstruction(offset, replacementInstruction);
            }
        }
        else if (branchTargetFinder.isSubroutineStart(offset))
        {
            if (DEBUG)
            {
                System.out.println("    Replacing first subroutine instruction "+variableInstruction.toString(clazz, offset)+" by a label");
            }

            // Append a label at this offset instead of saving the subroutine
            // return address.
            codeAttributeComposer.appendLabel(offset);
        }
        else
        {
            // Append the instruction.
            codeAttributeComposer.appendInstruction(offset, variableInstruction);
        }
    }


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        byte opcode = branchInstruction.opcode;
        if (opcode == Instruction.OP_JSR ||
            opcode == Instruction.OP_JSR_W)
        {
            int branchOffset = branchInstruction.branchOffset;
            int branchTarget = offset + branchOffset;

            // Is the subroutine ever returning?
            if (branchTargetFinder.isSubroutineReturning(branchTarget))
            {
                // Append a label at this offset instead of the subroutine invocation.
                codeAttributeComposer.appendLabel(offset);

                // Inline the invoked subroutine.
                inlineSubroutine(clazz,
                                 method,
                                 codeAttribute,
                                 offset,
                                 branchTarget);
            }
            else
            {
                if (DEBUG)
                {
                    System.out.println("Replacing subroutine invocation at ["+offset+"] by a simple branch");
                }

                // Replace the subroutine invocation by a simple branch.
                Instruction replacementInstruction =
                    new BranchInstruction(Instruction.OP_GOTO,
                                          branchOffset);

                codeAttributeComposer.appendInstruction(offset, replacementInstruction);
            }
        }
        else
        {
            // Append the instruction.
            codeAttributeComposer.appendInstruction(offset, branchInstruction);
        }
    }


    // Implementations for ExceptionInfoVisitor.

    public void visitExceptionInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, ExceptionInfo exceptionInfo)
    {
        int startPC   = Math.max(exceptionInfo.u2startPC, clipStart);
        int endPC     = Math.min(exceptionInfo.u2endPC,   clipEnd);
        int handlerPC = exceptionInfo.u2handlerPC;
        int catchType = exceptionInfo.u2catchType;

        // Exclude any subroutine invocations that jump out of the try block,
        // by adding a try block before (and later on, after) each invocation.
        for (int offset = startPC; offset < endPC; offset++)
        {
            if (branchTargetFinder.isSubroutineInvocation(offset))
            {
                Instruction instruction = InstructionFactory.create(codeAttribute.code, offset);
                int instructionLength = instruction.length(offset);

                // Is it a subroutine invocation?
                if (!exceptionInfo.isApplicable(offset + ((BranchInstruction)instruction).branchOffset))
                {
                    if (DEBUG)
                    {
                        System.out.println("  Appending extra exception ["+startPC+" -> "+offset+"] -> "+handlerPC);
                    }

                    // Append a try block that ends before the subroutine invocation.
                    codeAttributeComposer.appendException(new ExceptionInfo(startPC,
                                                                            offset,
                                                                            handlerPC,
                                                                            catchType));

                    // The next try block will start after the subroutine invocation.
                    startPC = offset + instructionLength;
                }
            }
        }

        if (DEBUG)
        {
            if (startPC == exceptionInfo.u2startPC &&
                endPC   == exceptionInfo.u2endPC)
            {
                System.out.println("  Appending exception ["+startPC+" -> "+endPC+"] -> "+handlerPC);
            }
            else
            {
                System.out.println("  Appending clipped exception ["+exceptionInfo.u2startPC+" -> "+exceptionInfo.u2endPC+"] ~> ["+startPC+" -> "+endPC+"] -> "+handlerPC);
            }
        }

        // Append the exception. Note that exceptions with empty try blocks
        // are automatically ignored.
        codeAttributeComposer.appendException(new ExceptionInfo(startPC,
                                                                endPC,
                                                                handlerPC,
                                                                catchType));
    }
}
