package proguard.evaluation.stateTrackers;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.instruction.Instruction;
import proguard.evaluation.BasicBranchUnit;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.evaluation.Variables;
import proguard.evaluation.value.InstructionOffsetValue;

import java.util.List;

public interface PartialEvaluatorStateTracker
{
    /************************
     * Code attribute level *
     ************************/
    default void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters) {}

    default void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                   PartialEvaluator evaluator, Throwable cause) {}

    /**************
     * Exceptions *
     **************/
    default void startExceptionHandlingForBlock(Clazz clazz, Method method, int startOffset, int endOffset) {}

    default void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info) {}

    default void registerUnusedExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info) {}


    /***********
     * Results *
     ***********/
    default void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator) {}


    /***************************
     * Instruction block level *
     ***************************/
    default void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                       TracedVariables startVariables, TracedStack startStack, int startOffset) {}

    default void startBranchCodeBlockEvaluation(List<PartialEvaluator.InstructionBlock> branchStack) {}

    default void instructionBlockDone(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables startVariables, TracedStack startStack, int startOffset) {}


    /*********************
     * Instruction level *
     *********************/
    default void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                      TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount) {}

    default void generalizeInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                            TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount) {}

    default void startInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                            TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount) {}

    default void afterInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                            TracedVariables variablesAfter, TracedStack stackAfter, BasicBranchUnit branchUnit,
                                            InstructionOffsetValue branchTarget) {}

    default void definitiveBranch(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                  TracedVariables variablesAfter, TracedStack stackAfter, InstructionOffsetValue branchTargets) {}

    default void registerAlternativeBranch(Clazz clazz, Method method, int fromInstructionOffset, Instruction fromInstruction,
                                           TracedVariables variablesAfter, TracedStack stackAfter,
                                           int branchIndex, int branchTargetCount, int offset) {}


    /***************
     * Subroutines *
     ***************/
    default void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack,
                                 int subroutineStart, int subroutineEnd) {}

    default void generalizeSubroutine(Clazz clazz, Method method, TracedVariables startVariables,
                                      TracedStack startStack, int subroutineStart, int subroutineEnd) {}

    default void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter,
                               int subroutineStart, int subroutineEnd) {}
}
