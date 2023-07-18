package proguard.evaluation.formatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.BasicBranchUnit;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.evaluation.Variables;
import proguard.evaluation.value.InstructionOffsetValue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Capable of printing machine-readable output (JSON) xp
 * {
 *      "codeAttribute": {
 *          clazz
 *          method
 *          instructions: {offset, instruction}[],
 *          variables
 *          blockEvaluations: {
 *              startOffset,
 *              variables,
 *              stack,
 *              evaluations: {
 *                  isSeenBefore,
 *                  isGeneralization,
 *                  timesSeen,
 *                  offset,
 *                  instruction
 *                  updatedEvaluationStack,
 *                  variablesBefore
 *                  stackBefore
 *              }[]
 *          }[]
 *      }[]
 *      "error"?: {
 *          clazz
 *          method
 *          offset: int,
 *          message: string,
 *          stacktrace: string,
 *      }
 * }
 */

public class MachinePrinter implements PartialEvaluatorStateTracker
{
    static class StateTracker
    {
        static class CodeAttributeTracker
        {
            static class InstructionBlock
            {
                public String variables;
                public String stack;
                public int startOffset;

                public InstructionBlock(String variables, String stack, int startOffset)
                {
                    this.variables=variables;
                    this.stack=stack;
                    this.startOffset=startOffset;
                }
            }

            static class InstructionTracker {
                public int offset;
                public String instruction;

                public InstructionTracker(int offset, String instruction)
                {
                    this.offset=offset;
                    this.instruction=instruction;
                }
            }

            static class BlockEvaluationTracker {
                static class InstructionEvaluationTracker
                {
                    public Boolean isSeenBefore;
                    public Boolean isGeneralization;
                    public Integer timesSeen;
                    public String instruction;
                    public Integer instructionOffset;
                    public List<InstructionBlock> updatedEvaluationStack;
                    public String variablesBefore;
                    public String stackBefore;

                    public static InstructionEvaluationTracker seenIndicator() {
                        return new InstructionEvaluationTracker(
                                true, null, null, null,
                                null, null, null, null);
                    }

                    public static InstructionEvaluationTracker generalizationIndicator(int timesSeen) {
                        return new InstructionEvaluationTracker(null, true, timesSeen, null,
                                null, null, null, null);
                    }

                    public static InstructionEvaluationTracker instructionTracker(
                            String instruction, int instructionOffset, List<InstructionBlock> evaluationBlockStack,
                            String variablesBefore, String stackBefore) {
                        return new InstructionEvaluationTracker(null, null, null,
                                instruction, instructionOffset, evaluationBlockStack, variablesBefore, stackBefore);
                    }

                    public InstructionEvaluationTracker(Boolean isSeenBefore, Boolean isGeneralization, Integer timesSeen, String instruction,
                                                        Integer instructionOffset, List<InstructionBlock> evaluationBlockStack,
                                                        String variablesBefore, String stackBefore)
                    {
                        this.isSeenBefore=isSeenBefore;
                        this.isGeneralization=isGeneralization;
                        this.instruction=instruction;
                        this.instructionOffset=instructionOffset;
                        this.updatedEvaluationStack=evaluationBlockStack;
                        this.variablesBefore=variablesBefore;
                        this.stackBefore=stackBefore;
                    }
                }

                public List<InstructionEvaluationTracker> evaluations = new ArrayList<>();
                public String startVariables;
                public String startStack;
                public int startOffset;

                public BlockEvaluationTracker(String startVariables, String startStack, int startOffset)
                {
                    this.startVariables=startVariables;
                    this.startStack=startStack;
                    this.startOffset=startOffset;
                }

                public InstructionEvaluationTracker getLastEvaluation() {
                    if (evaluations.isEmpty()) {
                        return null;
                    }
                    return evaluations.get(evaluations.size() - 1);
                }
            }

            public String clazz;
            public String method;
            public List<InstructionTracker> instructions = new ArrayList<>();
            public String parameters;
            public List<BlockEvaluationTracker> blockEvaluations = new ArrayList<>();

            public CodeAttributeTracker(String clazz, String method, String startVariables)
            {
                this.clazz=clazz;
                this.method=method;
                this.parameters=startVariables;
            }

            public BlockEvaluationTracker getLastBlockEvaluation() {
                if (blockEvaluations.isEmpty()) {
                    return null;
                }
                return blockEvaluations.get(blockEvaluations.size() - 1);
            }
        }

        static class ErrorTracker
        {
            public String clazz;
            public String method;
            public int instructionOffset;
            public String message;
            String cause;

            public ErrorTracker(String clazz, String method, int instructionOffset, String message, String cause)
            {
                this.clazz=clazz;
                this.method=method;
                this.instructionOffset=instructionOffset;
                this.message=message;
                this.cause=cause;
            }
        }

        public final List<CodeAttributeTracker> codeAttributes=new ArrayList<>();
        public ErrorTracker error;

        public CodeAttributeTracker getLastCodeAttribute() {
            if (codeAttributes.isEmpty()) {
                return null;
            }
            return codeAttributes.get(codeAttributes.size() - 1);
        }
    }

    private final Gson gson=new GsonBuilder().setPrettyPrinting().create();

    private StateTracker stateTracker=new StateTracker();

    @Override
    public void registerUnusedExceptionHandler(int startPC, int endPC, ExceptionInfo info)
    {

    }

    @Override
    public void registerExceptionHandler(int startPC, int endPC, int handlerPC)
    {

    }

    @Override
    public void startExceptionHandling(int startOffset, int endOffset)
    {

    }

    @Override
    public void generalizeSubroutine(int subroutineStart, int subroutineEnd)
    {

    }

    @Override
    public void endSubroutine(int subroutineStart, int subroutineEnd)
    {

    }

    @Override
    public void startSubroutine(int subroutineStart, int subroutineEnd)
    {

    }

    @Override
    public void instructionBlockDone(int startOffset)
    {

    }

    @Override
    public void definitiveBranch(int instructionOffset, InstructionOffsetValue branchTargets)
    {

    }

    @Override
    public void registerAlternativeBranch(int index, int branchTargetCount, int instructionOffset, InstructionOffsetValue offsetValue)
    {

    }

    @Override
    public void afterInstructionEvaluation(BasicBranchUnit branchUnit, int instructionOffset, TracedVariables variables, TracedStack stack, InstructionOffsetValue branchTarget)
    {

    }

    @Override
    public void startInstructionEvaluation(Instruction instruction, Clazz clazz, int instructionOffset,
                                           TracedVariables variablesBefore, TracedStack stackBefore)
    {
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker evaluation =
                stateTracker.getLastCodeAttribute().getLastBlockEvaluation().getLastEvaluation();
        if (evaluation != null && evaluation.isGeneralization != null && evaluation.isGeneralization) {
            evaluation.instruction = instruction.toString();
            evaluation.instructionOffset = instructionOffset;
            evaluation.variablesBefore = variablesBefore.toString();
            evaluation.stackBefore = stackBefore.toString();
            // TODO: add updatedEvaluationStack
        } else
        {
            stateTracker.getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                    StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.instructionTracker(
                            instruction.toString(), instructionOffset, null,
                            variablesBefore.toString(), stackBefore.toString()
                    )
            );
        }
    }

    @Override
    public void generalizeInstructionBlock(int evaluationCount)
    {
        stateTracker.getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.generalizationIndicator(evaluationCount));
    }

    @Override
    public void skipInstructionBlock()
    {
        stateTracker.getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.seenIndicator());
    }

    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables variables, TracedStack stack, int startOffset)
    {
        stateTracker.getLastCodeAttribute().blockEvaluations.add(
            new StateTracker.CodeAttributeTracker.BlockEvaluationTracker(
                    variables.toString(), stack.toString(), startOffset
        ));
    }

    @Override
    public void printStartBranchCodeBlockEvaluation(int stackSize)
    {

    }

    @Override
    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {

    }

    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        stateTracker.codeAttributes.add(new StateTracker.CodeAttributeTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz), parameters.toString()
        ));
        byte[] code = codeAttribute.code;
        int offset = 0;
        while (offset < code.length) {
            Instruction instruction = InstructionFactory.create(code, offset);
            stateTracker.getLastCodeAttribute().instructions.add(
                new StateTracker.CodeAttributeTracker.InstructionTracker(
                    offset, instruction.toString()
            ));

            offset += instruction.length(offset);
        }
    }

    @Override
    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator, Throwable cause)
    {
        stateTracker.error = new StateTracker.ErrorTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz),
                stateTracker.getLastCodeAttribute().getLastBlockEvaluation().getLastEvaluation().instructionOffset
                , cause.getMessage(), null
        );
    }

    public void printState()
    {
        System.out.println(gson.toJson(stateTracker));
    }

    public void writeState() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("pe-state.json"));
            writer.write(gson.toJson(stateTracker));

            writer.close();
        }
        catch (IOException ex) {
            // Do nothing
        }
    }
}
