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
