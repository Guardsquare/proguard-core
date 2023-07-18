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
    // Code attribute level:
    void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters);
    void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator, Throwable cause);

    // Exceptions
    void startExceptionHandling(int startOffset, int endOffset);
    void registerExceptionHandler(int startPC, int endPC, int handlerPC);
    void registerUnusedExceptionHandler(int startPC, int endPC, ExceptionInfo info);


    // Results
    void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator);


    // Instruction block level:
    void startInstructionBlock(Clazz clazz,
                               Method method,
                               CodeAttribute codeAttribute,
                               TracedVariables variables,
                               TracedStack stack,
                               int startOffset);
    void printStartBranchCodeBlockEvaluation(int stackSize);
    void instructionBlockDone(int startOffset);


    // Instruction level:
    void skipInstructionBlock();
    void generalizeInstructionBlock(int evaluationCount);
    void startInstructionEvaluation(Instruction instruction, Clazz clazz, int instructionOffset,
                                    TracedVariables variablesBefore, TracedStack stackBefore);
    void afterInstructionEvaluation(BasicBranchUnit branchUnit, int instructionOffset,
                                    TracedVariables variables, TracedStack stack,
                                    InstructionOffsetValue branchTarget);
    void definitiveBranch(int instructionOffset, InstructionOffsetValue branchTargets);
    void registerAlternativeBranch(int index, int branchTargetCount, int instructionOffset,
                                   InstructionOffsetValue offsetValue, TracedVariables variables,
                                   TracedStack stack, int offset);


    // Subroutine
    void generalizeSubroutine(int subroutineStart, int subroutineEnd);
    void startSubroutine(int subroutineStart, int subroutineEnd);
    void endSubroutine(int subroutineStart, int subroutineEnd);
}
