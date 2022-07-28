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
package proguard.classfile.editor;

import proguard.classfile.*;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.instruction.*;
import proguard.classfile.instruction.visitor.InstructionVisitor;

/**
 * This {@link InstructionVisitor} writes out the instructions that it visits,
 * collecting instructions that have to be widened. As an {@link AttributeVisitor},
 * it then applies the collected changes. The process will be repeated
 * recursively, if necessary. The caller still has to update the frame sizes.
 *
 * @author Eric Lafortune
 */
public class InstructionWriter
implements   InstructionVisitor,
             AttributeVisitor
{
    //*
    private static final boolean DEBUG = false;
    /*/
    public  static       boolean DEBUG = System.getProperty("iw") != null;
    //*/


    private int codeLength;

    private CodeAttributeEditor codeAttributeEditor;


    /**
     * Resets the accumulated code changes for a given anticipated maximum
     * code length. If necessary, the size may still be extended while
     * editing the code, with {@link #extend(int)}.
     * @param codeLength the length of the code that will be edited next.
     */
    public void reset(int codeLength)
    {
        this.codeLength = codeLength;

        if (codeAttributeEditor != null)
        {
            codeAttributeEditor.reset(codeLength);
        }
    }


    /**
     * Extends the size of the accumulated code.
     * @param codeLength the length of the code that will be edited next.
     */
    public void extend(int codeLength)
    {
        this.codeLength = codeLength;

        if (codeAttributeEditor != null)
        {
            codeAttributeEditor.extend(codeLength);
        }
    }


    // Implementations for InstructionVisitor.

    public void visitSimpleInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SimpleInstruction simpleInstruction)
    {
        // Try to write out the instruction.
        // Simple instructions should always fit.
        simpleInstruction.write(codeAttribute, offset);
    }


    public void visitConstantInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, ConstantInstruction constantInstruction)
    {
        try
        {
            // Try to write out the instruction.
            constantInstruction.write(codeAttribute, offset);
        }
        catch (IllegalArgumentException exception)
        {
            // Create a new constant instruction that will fit.
            Instruction replacementInstruction =
                new ConstantInstruction(constantInstruction.opcode,
                                        constantInstruction.constantIndex,
                                        constantInstruction.constant);

            if (DEBUG)
            {
                System.out.println("  "+constantInstruction.toString(clazz, offset)+" will be widened to "+replacementInstruction.toString());
            }

            replaceInstruction(offset, replacementInstruction);

            // Write out a dummy constant instruction for now.
            constantInstruction.constantIndex = 0;
            constantInstruction.constant      = 0;
            constantInstruction.write(codeAttribute, offset);
        }
    }


    public void visitVariableInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, VariableInstruction variableInstruction)
    {
        try
        {
            // Try to write out the instruction.
            variableInstruction.write(codeAttribute, offset);
        }
        catch (IllegalArgumentException exception)
        {
            // Create a new variable instruction that will fit.
            Instruction replacementInstruction =
                new VariableInstruction(variableInstruction.opcode,
                                        variableInstruction.variableIndex,
                                        variableInstruction.constant);

            replaceInstruction(offset, replacementInstruction);

            if (DEBUG)
            {
                System.out.println("  "+variableInstruction.toString(clazz, offset)+" will be widened to "+replacementInstruction.toString());
            }

            // Write out a dummy variable instruction for now.
            variableInstruction.variableIndex = 0;
            variableInstruction.constant      = 0;
            variableInstruction.write(codeAttribute, offset);
        }
    }


    public void visitBranchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, BranchInstruction branchInstruction)
    {
        try
        {
            // Try to write out the instruction.
            branchInstruction.write(codeAttribute, offset);
        }
        catch (IllegalArgumentException exception)
        {
            // Create a new unconditional branch that will fit.
            Instruction replacementInstruction =
                new BranchInstruction(Instruction.OP_GOTO_W,
                                      branchInstruction.branchOffset);

            // Create a new instruction that will fit.
            switch (branchInstruction.opcode)
            {
                default:
                {
                    // Create a new branch instruction that will fit.
                    replacementInstruction =
                        new BranchInstruction(branchInstruction.opcode,
                                              branchInstruction.branchOffset);

                    break;
                }

                // Some special cases, for which a wide branch doesn't exist.
                case Instruction.OP_IFEQ:
                case Instruction.OP_IFNE:
                case Instruction.OP_IFLT:
                case Instruction.OP_IFGE:
                case Instruction.OP_IFGT:
                case Instruction.OP_IFLE:
                case Instruction.OP_IFICMPEQ:
                case Instruction.OP_IFICMPNE:
                case Instruction.OP_IFICMPLT:
                case Instruction.OP_IFICMPGE:
                case Instruction.OP_IFICMPGT:
                case Instruction.OP_IFICMPLE:
                case Instruction.OP_IFACMPEQ:
                case Instruction.OP_IFACMPNE:
                {
                    // Insert the complementary conditional branch.
                    Instruction complementaryConditionalBranch =
                        new BranchInstruction((byte)(((branchInstruction.opcode+1) ^ 1) - 1),
                                              (1+2));

                    insertBeforeInstruction(offset, complementaryConditionalBranch);

                    // Create a new unconditional branch that will fit.
                    break;
                }

                case Instruction.OP_IFNULL:
                case Instruction.OP_IFNONNULL:
                {
                    // Insert the complementary conditional branch.
                    Instruction complementaryConditionalBranch =
                        new BranchInstruction((byte)(branchInstruction.opcode ^ 1),
                                              (1+2));

                    insertBeforeInstruction(offset, complementaryConditionalBranch);

                    // Create a new unconditional branch that will fit.
                    break;
                }
            }

            if (DEBUG)
            {
                System.out.println("  "+branchInstruction.toString(clazz, offset)+" will be widened to "+replacementInstruction.toString());
            }

            replaceInstruction(offset, replacementInstruction);

            // Write out a dummy branch instruction for now.
            branchInstruction.branchOffset = 0;
            branchInstruction.write(codeAttribute, offset);
        }
    }


    public void visitAnySwitchInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, SwitchInstruction switchInstruction)
    {
        // Try to write out the instruction.
        // Switch instructions should always fit.
        switchInstruction.write(codeAttribute, offset);
    }


    // Implementations for AttributeVisitor.

    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Avoid doing any work if nothing is changing anyway.
        if (codeAttributeEditor != null)
        {
            if (DEBUG)
            {
                System.out.println("InstructionWriter: widening instructions in "+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz));
            }

            // Apply the collected expansions.
            codeAttributeEditor.visitCodeAttribute(clazz, method, codeAttribute);

            // Don't keep the editor around. We're assuming it won't be needed
            // very often, so we don't want to be resetting it all the time.
            codeAttributeEditor = null;
        }
    }


    // Small utility methods.

    /**
     * Remembers to place the given instruction right before the instruction
     * at the given offset.
     */
    private void insertBeforeInstruction(int instructionOffset, Instruction instruction)
    {
        ensureCodeAttributeEditor();

        // Replace the instruction.
        codeAttributeEditor.insertBeforeInstruction(instructionOffset, instruction);
    }


    /**
     * Remembers to replace the instruction at the given offset by the given
     * instruction.
     */
    private void replaceInstruction(int instructionOffset, Instruction instruction)
    {
        ensureCodeAttributeEditor();

        // Replace the instruction.
        codeAttributeEditor.replaceInstruction(instructionOffset, instruction);
    }


    /**
     * Remembers to place the given instruction right after the instruction
     * at the given offset.
     */
    private void insertAfterInstruction(int instructionOffset, Instruction instruction)
    {
        ensureCodeAttributeEditor();

        // Replace the instruction.
        codeAttributeEditor.insertAfterInstruction(instructionOffset, instruction);
    }


    /**
     * Makes sure there is a code attribute editor for the given code attribute.
     */
    private void ensureCodeAttributeEditor()
    {
        if (codeAttributeEditor == null)
        {
            codeAttributeEditor = new CodeAttributeEditor(false, true);
            codeAttributeEditor.reset(codeLength);
        }
    }
}
