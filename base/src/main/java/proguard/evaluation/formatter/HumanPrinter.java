package proguard.evaluation.formatter;

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

public class HumanPrinter
{
    public void registerUnusedExceptionHandler(int startPC, int endPC, ExceptionInfo info) {
        System.out.println("No information for partial evaluation of exception ["+startPC +" -> "+endPC +": "+info.u2handlerPC+"]");
    }

    public void registerExceptionHandler(int startPC, int endPC, int handlerPC)
    {
        System.out.println("Evaluating exception ["+startPC+" -> "+endPC+": "+handlerPC+"]:");
    }

    public void startExceptionHandling(int startOffset, int endOffset)
    {
        System.out.println("Evaluating exceptions covering ["+startOffset+" -> "+endOffset+"]:");
    }

    public void generalizeSubroutine(int subroutineStart, int subroutineEnd)
    {
        System.out.println("Ending subroutine from "+subroutineStart+" to "+subroutineEnd);
    }

    public void endSubroutine(int subroutineStart, int subroutineEnd)
    {
        System.out.println("Ending subroutine from "+subroutineStart+" to "+subroutineEnd);
    }

    public void startSubroutine(int subroutineStart, int subroutineEnd)
    {
        System.out.println("Evaluating subroutine from "+subroutineStart+" to "+subroutineEnd);
    }

    public void instructionBlockDone(int startOffset)
    {
        System.out.println("Ending processing of instruction block starting at ["+startOffset+"]");
    }

    public void definitiveBranch(int instructionOffset, InstructionOffsetValue branchTargets)
    {
        System.out.println("Definite branch from ["+instructionOffset+"] to ["+branchTargets.instructionOffset(0)+"]");
    }

    public void registerAlternativeBranch(int index, int branchTargetCount, int instructionOffset, InstructionOffsetValue offsetValue)
    {
        System.out.println("Pushing alternative branch #"+index+" out of "+branchTargetCount+
                ", from ["+instructionOffset+"] to ["+offsetValue+"]");
    }

    public void afterInstructionEvaluation(BasicBranchUnit branchUnit, int instructionOffset,
                                           TracedVariables variables, TracedStack stack,
                                           InstructionOffsetValue branchTarget)
    {
        InstructionOffsetValue branchTargets=branchUnit.getTraceBranchTargets();
        int branchTargetCount=branchTargets.instructionOffsetCount();
        if (branchUnit.wasCalled())
        {
            System.out.println("     is branching to "+branchTargets);
        }
        if (branchTarget != null)
        {
            System.out.println("     has up till now been branching to "+branchTarget);
        }

        System.out.println(" Vars:  "+variables);
        System.out.println(" Stack: "+stack);
    }

    public void startInstructionEvaluation(Instruction instruction, Clazz clazz, int instructionOffset)
    {
        System.out.println(instruction.toString(clazz, instructionOffset));
    }

    public void generalizeInstructionBlock(int evaluationCount)
    {
        System.out.println("Generalizing current context after "+evaluationCount+" evaluations");
    }

    public void skipInstructionBlock()
    {
        System.out.println("Repeated variables, stack, and branch targets");
    }

    public void startInstructionBlock(Clazz clazz,
                                      Method method,
                                      CodeAttribute codeAttribute,
                                      TracedVariables variables,
                                      TracedStack stack,
                                      int startOffset)
    {
        System.out.println("Instruction block starting at ["+startOffset+"] in "+
                ClassUtil.externalFullMethodDescription(clazz.getName(),
                        0,
                        method.getName(clazz),
                        method.getDescriptor(clazz)));
        System.out.println("Init vars:  "+variables);
        System.out.println("Init stack: "+stack);
    }

    public void printStartBranchCodeBlockEvaluation(int stackSize)
    {
        System.out.println("Popping alternative branch out of "+stackSize+" blocks");
    }

    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {
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

            Instruction instruction=InstructionFactory.create(codeAttribute.code,
                    offset);
            System.out.println(instruction.toString(clazz, offset));

            if (evaluator.isTraced(offset))
            {
//                    int initializationOffset = branchTargetFinder.initializationOffset(offset);
//                    if (initializationOffset != NONE)
//                    {
//                        System.out.println("     is to be initialized at ["+initializationOffset+"]");
//                    }

                InstructionOffsetValue branchTargets=evaluator.branchTargets(offset);
                if (branchTargets != null)
                {
                    System.out.println("     has overall been branching to "+branchTargets);
                }

                System.out.println("  Vars:  "+evaluator.getVariablesAfter(offset));
                System.out.println("  Stack: "+evaluator.getStackAfter(offset));
            }

            offset+=instruction.length(offset);
        }
        while (offset < codeAttribute.u4codeLength);
    }

    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        System.out.println();
        System.out.println("Partial evaluation: "+clazz.getName()+"."+method.getName(clazz)+method.getDescriptor(clazz));
        System.out.println("  Max locals = "+codeAttribute.u2maxLocals);
        System.out.println("  Max stack  = "+codeAttribute.u2maxStack);
        System.out.println("  Params: "+parameters);

    }

    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
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

            Instruction instruction=InstructionFactory.create(codeAttribute.code,
                    offset);
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

            offset+=instruction.length(offset);
        }
        while (offset < codeAttribute.u4codeLength);
    }
}
