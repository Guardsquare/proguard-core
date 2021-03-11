/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2020 Guardsquare NV
 */

package proguard.classfile.instruction.visitor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.*;

/**
 * This InstructionVisitor delegates its visits to one of two
 * InstructionVisitors, based on whether the opcode of the visited
 * instruction matches the list of passed opcodes.
 *
 * @author Tim Van Den Broecke
 */
public class InstructionOpCodeFilter
implements InstructionVisitor
{
    private final int[]              opcodes;
    private final InstructionVisitor acceptedVisitor;
    private final InstructionVisitor rejectedVisitor;


    public InstructionOpCodeFilter(int[]              opcodes,
                                   InstructionVisitor acceptedVisitor)
    {
        this(opcodes, acceptedVisitor, null);
    }


    public InstructionOpCodeFilter(int[]              opcodes,
                                   InstructionVisitor acceptedVisitor,
                                   InstructionVisitor rejectedVisitor)
    {
        this.opcodes         = opcodes;
        this.acceptedVisitor = acceptedVisitor;
        this.rejectedVisitor = rejectedVisitor;
    }


    // Implementations for InstructionVisitor.

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        InstructionVisitor delegate = getDelegateVisitor(instruction.opcode);
        if (delegate != null)
        {
            instruction.accept(clazz, method, codeAttribute, offset, delegate);
        }
    }


    // Private utility methods.

    private InstructionVisitor getDelegateVisitor(int opcode)
    {
        for (int allowedOpcode : opcodes)
        {
            if (opcode == allowedOpcode)
            {
                return acceptedVisitor;
            }
        }
        return rejectedVisitor;
    }
}
