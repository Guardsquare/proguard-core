/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
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

package proguard.evaluation.util;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassPrinter;
import proguard.evaluation.BasicBranchUnit;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.evaluation.Variables;
import proguard.evaluation.value.InstructionOffsetValue;

import java.util.List;

public class DebugPrinter implements PartialEvaluatorStateTracker
{
    private final boolean printDebugInfo;
    private final boolean printDebugResults;

    public DebugPrinter(boolean printDebugInfo, boolean printDebugResults)
    {
        this.printDebugInfo = printDebugInfo;
        this.printDebugResults = printDebugResults;
    }

    // region Code attribute level

    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        if (printDebugInfo)
        {
            System.out.println();
            System.out.println("Partial evaluation: "+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz));
            System.out.println("  Max locals = "+codeAttribute.u2maxLocals);
            System.out.println("  Max stack  = "+codeAttribute.u2maxStack);
            System.out.println("  Params: "+parameters);
        }
    }

    @Override
    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator, Throwable cause)
    {
        if (printDebugInfo)
        {
            method.accept(clazz, new ClassPrinter());

            System.out.println("Evaluation results:");

            int offset=0;
            do
            {
                if (evaluator.isBranchOrExceptionTarget(offset))
                {
                    System.out.println("Branch target from ["+evaluator.branchOrigins(offset)+"]:");
                    if (evaluator.isTraced(offset))
                    {
                        System.out.println("  Vars:  "+evaluator.getVariablesBefore(offset));
                        System.out.println("  Stack: "+evaluator.getStackBefore(offset));
                    }
                }

                Instruction instruction=InstructionFactory.create(codeAttribute.code, offset);
                System.out.println(instruction.toString(clazz, offset));

                if (evaluator.isTraced(offset))
                {
//                        int initializationOffset = branchTargetFinder.initializationOffset(offset);
//                        if (initializationOffset != NONE)
//                        {
//                            System.out.println("     is to be initialized at ["+initializationOffset+"]");
//                        }

                    InstructionOffsetValue branchTargets=evaluator.branchTargets(offset);
                    if (branchTargets != null)
                    {
                        System.out.println("     has overall been branching to "+branchTargets);
                    }

                    System.out.println("  Vars:  "+evaluator.getVariablesAfter(offset));
                    System.out.println("  Stack: "+evaluator.getStackAfter(offset));
                }

                offset += instruction.length(offset);
            }
            while (offset < codeAttribute.u4codeLength);
        }
    }

    // endregion

    // region Exception handling

    @Override
    public void startExceptionHandlingForBlock(Clazz clazz, Method method, int startOffset, int endOffset)
    {
        if (printDebugInfo)
        {
            System.out.println("Evaluating exceptions covering ["+startOffset+" -> "+endOffset+"]:");
        }
    }

    @Override
    public void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        if (printDebugInfo)
        {
            System.out.println("Evaluating exception ["+startPC+" -> "+endPC+": "+info.u2handlerPC+"]:");
        }
    }

    @Override
    public void registerUnusedExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        if (printDebugInfo)
        {
            System.out.println("No information for partial evaluation of exception ["+startPC+" -> "+endPC+": "+info.u2handlerPC+"]");
        }
    }

    // endregion

    // region Results

    @Override
    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {
        if (printDebugResults)
        {
            System.out.println("Evaluation results:");

            int offset = 0;
            do
            {
                if (evaluator.isBranchOrExceptionTarget(offset))
                {
                    System.out.println("Branch target from ["+evaluator.branchOrigins(offset)+"]:");
                    if (evaluator.isTraced(offset))
                    {
                        System.out.println("  Vars:  "+evaluator.getVariablesBefore(offset));
                        System.out.println("  Stack: "+evaluator.getStackBefore(offset));
                    }
                }

                Instruction instruction=InstructionFactory.create(codeAttribute.code, offset);
                System.out.println(instruction.toString(clazz, offset));

                if (evaluator.isTraced(offset))
                {
//                    int initializationOffset = branchTargetFinder.initializationOffset(offset);
//                    if (initializationOffset != NONE)
//                    {
//                        System.out.println("     is to be initialized at ["+initializationOffset+"]");
//                    }

                    InstructionOffsetValue branchTargets = evaluator.branchTargets(offset);
                    if (branchTargets != null)
                    {
                        System.out.println("     has overall been branching to "+branchTargets);
                    }

                    System.out.println("  Vars:  "+evaluator.getVariablesAfter(offset));
                    System.out.println("  Stack: "+evaluator.getStackAfter(offset));
                }

                offset += instruction.length(offset);
            }
            while (offset < codeAttribute.u4codeLength);
        }
    }

    // endregion

    // region Instruction block level

    @Override
    public void startInstructionBlock(Clazz clazz,
                                      Method method,
                                      CodeAttribute codeAttribute,
                                      TracedVariables startVariables,
                                      TracedStack startStack,
                                      int startOffset)
    {
        if (printDebugInfo)
        {
            System.out.println("Instruction block starting at ["+startOffset+"] in "+
                    ClassUtil.externalFullMethodDescription(clazz.getName(), 0, method.getName(clazz),
                            method.getDescriptor(clazz)));
            System.out.println("Init vars:  "+startVariables);
            System.out.println("Init stack: "+startStack);
        }
    }

    @Override
    public void startBranchCodeBlockEvaluation(List<PartialEvaluator.InstructionBlock> branchStack)
    {
        if (printDebugInfo)
        {
            System.out.println("Popping alternative branch out of "+branchStack.size()+" blocks");
        }
    }

    @Override
    public void instructionBlockDone(Clazz clazz,
                                     Method method,
                                     CodeAttribute codeAttribute,
                                     TracedVariables startVariables,
                                     TracedStack startStack,
                                     int startOffset)
    {
        if (printDebugInfo)
        {
            System.out.println("Ending processing of instruction block starting at ["+startOffset+"]");
        }
    }

    // endregion

    // region Instruction level

    @Override
    public void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (printDebugInfo)
        {
            System.out.println("Repeated variables, stack, and branch targets");
        }
    }

    @Override
    public void generalizeInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (printDebugInfo)
        {
            System.out.println("Generalizing current context after "+evaluationCount+" evaluations");
        }
    }

    @Override
    public void startInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (printDebugInfo)
        {
            System.out.println(instruction.toString(clazz, instructionOffset));
        }
    }

    @Override
    public void afterInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesAfter, TracedStack stackAfter, BasicBranchUnit branchUnit,
                                           InstructionOffsetValue branchTarget)
    {
        if (printDebugInfo)
        {
            InstructionOffsetValue branchTargets = branchUnit.getTraceBranchTargets();
            if (branchUnit.wasCalled())
            {
                System.out.println("     is branching to "+branchTargets);
            }
            if (branchTarget != null)
            {
                System.out.println("     has up till now been branching to "+branchTarget);
            }

            System.out.println(" Vars:  "+variablesAfter);
            System.out.println(" Stack: "+stackAfter);
        }
    }

    @Override
    public void definitiveBranch(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesAfter, TracedStack stackAfter, InstructionOffsetValue branchTargets)
    {
        if (printDebugInfo)
        {
            System.out.println("Definite branch from ["+instructionOffset+"] to ["+branchTargets.instructionOffset(0)+"]");
        }
    }

    @Override
    public void registerAlternativeBranch(Clazz clazz, Method method, int fromInstructionOffset, Instruction fromInstruction, TracedVariables variablesAfter, TracedStack stackAfter, int branchIndex, int branchTargetCount,
                                          int offset)
    {
        if (printDebugInfo)
        {
            System.out.println("Pushing alternative branch #"+branchIndex+" out of "+branchTargetCount+
                    ", from ["+fromInstructionOffset+"] to ["+offset+"]");
        }
    }

    // endregion

    // region Subroutines

    @Override
    public void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd)
    {
        if (printDebugInfo)
        {
            System.out.println("Evaluating subroutine from "+subroutineStart+" to "+subroutineEnd);
        }
    }

    @Override
    public void registerSubroutineReturn(Clazz clazz, Method method, int returnOffset, TracedVariables returnVariables, TracedStack returnStack)
    {
        System.out.println("Subroutine will return to "+returnOffset+".");
    }

    @Override
    public void generalizeSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd)
    {
        if (printDebugInfo)
        {
            System.out.println("Ending subroutine from "+subroutineStart+" to "+subroutineEnd);
        }
    }

    @Override
    public void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter, int subroutineStart, int subroutineEnd)
    {
        if (printDebugInfo)
        {
            System.out.println("Ending subroutine from "+subroutineStart+" to "+subroutineEnd);
        }
    }

    // endregion
}
