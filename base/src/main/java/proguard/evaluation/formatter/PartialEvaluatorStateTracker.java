package proguard.evaluation.formatter;

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

public interface PartialEvaluatorStateTracker
{
    void registerUnusedExceptionHandler(int startPC, int endPC, ExceptionInfo info);

    void registerExceptionHandler(int startPC, int endPC, int handlerPC);

    void startExceptionHandling(int startOffset, int endOffset);

    void generalizeSubroutine(int subroutineStart, int subroutineEnd);

    void endSubroutine(int subroutineStart, int subroutineEnd);

    void startSubroutine(int subroutineStart, int subroutineEnd);

    void instructionBlockDone(int startOffset);

    void definitiveBranch(int instructionOffset, InstructionOffsetValue branchTargets);

    void registerAlternativeBranch(int index, int branchTargetCount, int instructionOffset, InstructionOffsetValue offsetValue);

    void afterInstructionEvaluation(BasicBranchUnit branchUnit, int instructionOffset,
                                    TracedVariables variables, TracedStack stack,
                                    InstructionOffsetValue branchTarget);

    void startInstructionEvaluation(Instruction instruction, Clazz clazz, int instructionOffset);

    void generalizeInstructionBlock(int evaluationCount);

    void skipInstructionBlock();

    void startInstructionBlock(Clazz clazz,
                               Method method,
                               CodeAttribute codeAttribute,
                               TracedVariables variables,
                               TracedStack stack,
                               int startOffset);

    void printStartBranchCodeBlockEvaluation(int stackSize);

    void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator);

    void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters);

    void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator);
}
