package proguard.evaluation.formatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        static class MethodTracker
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

            static class InstructionLevelTracker
            {
                public Boolean isSeenBefore;
                public Boolean isGeneralization;
                public Integer timesSeen;
                public String instruction;
                public Integer instructionOffset;
                public List<InstructionBlock> evaluationBlockStack;
                public String variablesBefore;
                public String stackBefore;

                public static InstructionLevelTracker seenIndicator() {
                    return new InstructionLevelTracker(
                            true, null, null, null,
                            null, null, null, null);
                }

                public static InstructionLevelTracker generalizationIndicator(int timesSeen) {
                    return new InstructionLevelTracker(null, true, timesSeen, null,
                            null, null, null, null);
                }

                public static InstructionLevelTracker instructionTracker(
                        String instruction, int instructionOffset, List<InstructionBlock> evaluationBlockStack,
                                                                         String variablesBefore, String stackBefore) {
                    return new InstructionLevelTracker(null, null, null,
                            instruction, instructionOffset, evaluationBlockStack, variablesBefore, stackBefore);
                }

                public InstructionLevelTracker(Boolean isSeenBefore, Boolean isGeneralization, Integer timesSeen, String instruction,
                                               Integer instructionOffset, List<InstructionBlock> evaluationBlockStack,
                                               String variablesBefore, String stackBefore)
                {
                    this.isSeenBefore=isSeenBefore;
                    this.isGeneralization=isGeneralization;
                    this.instruction=instruction;
                    this.instructionOffset=instructionOffset;
                    this.evaluationBlockStack=evaluationBlockStack;
                    this.variablesBefore=variablesBefore;
                    this.stackBefore=stackBefore;
                }
            }

            public String clazz;
            public String method;
            public String startVariables;
            public String startStack;
            public int startOffset;
            public List<InstructionLevelTracker> evaluations= new ArrayList<>();
            public List<InstructionTracker> instructions = new ArrayList<>();

            public MethodTracker(String clazz, String method, String startVariables,
                                 String startStack, int startOffset)
            {
                this.clazz=clazz;
                this.method=method;
                this.startVariables=startVariables;
                this.startStack=startStack;
                this.startOffset=startOffset;
            }

            public InstructionLevelTracker getLastInstruction() {
                if (evaluations.isEmpty()) {
                    return null;
                }
                return evaluations.get(evaluations.size() - 1);
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

        public MethodTracker getLastMethodTracker() {
            return methods.get(methods.size() - 1);
        }

        public final List<MethodTracker> methods=new ArrayList<>();
        public ErrorTracker error;
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
        StateTracker.MethodTracker.InstructionLevelTracker tracker = stateTracker.getLastMethodTracker().getLastInstruction();
        if (tracker != null && tracker.isGeneralization != null && tracker.isGeneralization) {
            tracker.instruction = instruction.toString();
            tracker.instructionOffset = instructionOffset;
            tracker.variablesBefore = variablesBefore.toString();
            tracker.stackBefore = stackBefore.toString();
        } else
        {
            stateTracker.getLastMethodTracker().evaluations.add(
                    StateTracker.MethodTracker.InstructionLevelTracker.instructionTracker(
                            instruction.toString(), instructionOffset, null,
                            variablesBefore.toString(), stackBefore.toString()
                    )
            );
        }
    }

    @Override
    public void generalizeInstructionBlock(int evaluationCount)
    {
        stateTracker.getLastMethodTracker().evaluations.add(
                StateTracker.MethodTracker.InstructionLevelTracker.generalizationIndicator(evaluationCount)
        );
    }

    @Override
    public void skipInstructionBlock()
    {
        stateTracker.getLastMethodTracker().evaluations.add(
                StateTracker.MethodTracker.InstructionLevelTracker.seenIndicator());
    }

    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute, TracedVariables variables, TracedStack stack, int startOffset)
    {
        stateTracker.methods.add(new StateTracker.MethodTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz), variables.toString(),
                stack.toString(), startOffset
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
        // TODO(mj): add code here
        System.out.println("NOOOOOOO");
    }

    @Override
    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator, Throwable cause)
    {
        stateTracker.error = new StateTracker.ErrorTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz),
                stateTracker.getLastMethodTracker().getLastInstruction().instructionOffset, cause.getMessage(), null
        );
    }

    public void printState()
    {
        System.out.println(gson.toJson(stateTracker));
    }
}
