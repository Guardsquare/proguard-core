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
    // Code attribute level:
    void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters);

    void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator, Throwable cause);

    // Exceptions
    void startExceptionHandlingForBlock(Clazz clazz, Method method, int startOffset, int endOffset);

    void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info);

    void registerUnusedExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info);


    // Results
    void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator);


    // Instruction block level:
    void startInstructionBlock(Clazz clazz,
                               Method method,
                               CodeAttribute codeAttribute,
                               TracedVariables startVariables,
                               TracedStack startStack,
                               int startOffset);

    void startBranchCodeBlockEvaluation(List<PartialEvaluator.InstructionBlockDTO> branchStack);

    void instructionBlockDone(Clazz clazz,
                              Method method,
                              CodeAttribute codeAttribute,
                              TracedVariables startVariables,
                              TracedStack startStack,
                              int startOffset);


    // Instruction level:
    void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                              TracedVariables variablesBefore, TracedStack stackBefore);

    void generalizeInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                    TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount);

    void startInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                    TracedVariables variablesBefore, TracedStack stackBefore);

    void afterInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                    TracedVariables variablesAfter, TracedStack stackAfter, BasicBranchUnit branchUnit,
                                    InstructionOffsetValue branchTarget);

    void definitiveBranch(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                          TracedVariables variablesAfter, TracedStack stackAfter, InstructionOffsetValue branchTargets);

    void registerAlternativeBranch(Clazz clazz, Method method, int fromInstructionOffset, Instruction fromInstruction,
                                   TracedVariables variablesAfter, TracedStack stackAfter,
                                   int branchIndex, int branchTargetCount, int offset);


    // Subroutine
    void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd);

    void generalizeSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd);

    void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter, int subroutineStart, int subroutineEnd);
}
