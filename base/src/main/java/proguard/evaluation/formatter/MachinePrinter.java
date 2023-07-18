package proguard.evaluation.formatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.BasicBranchUnit;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.Stack;
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
 *          variables: String(Value)[]
 *          blockEvaluations: {
 *              startOffset,
 *              variables,
 *              stack,
 *              exceptionInfo: { startOffset, endOffset, catchType (just string name) }
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
                public List<String> variables;
                public List<String> stack;
                public int startOffset;

                public InstructionBlock(List<String> variables, List<String> stack, int startOffset)
                {
                    this.variables=variables;
                    this.stack=stack;
                    this.startOffset=startOffset;
                }
            }

            static class ExceptionHandlerInfo {
                public int catchStartOffset;
                public int catchEndOffset;
                public String catchType;

                public ExceptionHandlerInfo(int catchStartOffset, int catchEndOffset, String catchType)
                {
                    this.catchStartOffset=catchStartOffset;
                    this.catchEndOffset=catchEndOffset;
                    this.catchType=catchType;
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
                    public List<String> variablesBefore;
                    public List<String> stackBefore;

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
                            List<String> variablesBefore, List<String> stackBefore) {
                        return new InstructionEvaluationTracker(null, null, null,
                                instruction, instructionOffset, evaluationBlockStack, variablesBefore, stackBefore);
                    }

                    public InstructionEvaluationTracker(Boolean isSeenBefore, Boolean isGeneralization, Integer timesSeen, String instruction,
                                                        Integer instructionOffset, List<InstructionBlock> evaluationBlockStack,
                                                        List<String> variablesBefore, List<String> stackBefore)
                    {
                        this.isSeenBefore=isSeenBefore;
                        this.isGeneralization=isGeneralization;
                        this.timesSeen=timesSeen;
                        this.instruction=instruction;
                        this.instructionOffset=instructionOffset;
                        this.updatedEvaluationStack=evaluationBlockStack;
                        this.variablesBefore=variablesBefore;
                        this.stackBefore=stackBefore;
                    }
                }

                public List<InstructionEvaluationTracker> evaluations = new ArrayList<>();
                public ExceptionHandlerInfo exceptionHandlerInfo;
                public List<String> startVariables;
                public List<String> startStack;
                public int startOffset;

                public BlockEvaluationTracker(List<String> startVariables, List<String> startStack, int startOffset)
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
            public List<String> parameters;
            public List<BlockEvaluationTracker> blockEvaluations = new ArrayList<>();
            public List<InstructionBlock> lastEvaluationStack = new ArrayList<>();

            public CodeAttributeTracker(String clazz, String method, List<String> parameters)
            {
                this.clazz=clazz;
                this.method=method;
                this.parameters=parameters;
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

    private final StateTracker stateTracker = new StateTracker();

    private List<String> formatValueList(Variables variables) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            res.add(variables.getValue(i).toString());
        }
        return res;
    }

    private List<String> formatValueList(Stack stack) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < stack.size(); i++) {
            res.add(stack.getBottom(i).toString());
        }
        return res;
    }

    // Code attribute level:
    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        stateTracker.codeAttributes.add(new StateTracker.CodeAttributeTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz), formatValueList(parameters)
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


    // Exceptions


    @Override
    public void startExceptionHandling(int startOffset, int endOffset)
    {
        System.out.println("temp");

    }

    @Override
    public void registerExceptionHandler(int startPC, int endPC, int handlerPC, ExceptionInfo info, Clazz clazz)
    {
        stateTracker.getLastCodeAttribute().blockEvaluations.add(new StateTracker.CodeAttributeTracker.BlockEvaluationTracker(
                null, null, handlerPC
        ));

        ClassConstant constant =(ClassConstant) ((ProgramClass) clazz).getConstant(info.u2catchType);
        stateTracker.getLastCodeAttribute().getLastBlockEvaluation().exceptionHandlerInfo =
                new StateTracker.CodeAttributeTracker.ExceptionHandlerInfo(
                        startPC, endPC, constant == null ? "java/lang/Throwable" : constant.getName(clazz)
                );
    }

    @Override
    public void registerUnusedExceptionHandler(int startPC, int endPC, ExceptionInfo info)
    {
        System.out.println("temp");
    }


    // Results
    @Override
    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {

    }



    // Instruction block level
    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables variables, TracedStack stack, int startOffset)
    {
        if (!stateTracker.getLastCodeAttribute().lastEvaluationStack.isEmpty()) {
            stateTracker.getLastCodeAttribute().lastEvaluationStack.remove(stateTracker.getLastCodeAttribute().lastEvaluationStack.size() - 1);
        }
        stateTracker.getLastCodeAttribute().blockEvaluations.add(
                new StateTracker.CodeAttributeTracker.BlockEvaluationTracker(
                        formatValueList(variables), formatValueList(stack), startOffset
                ));
    }

    @Override
    public void printStartBranchCodeBlockEvaluation(int stackSize)
    {

    }

    @Override
    public void instructionBlockDone(int startOffset)
    {

    }


    // Instruction level:
    @Override
    public void skipInstructionBlock()
    {
        stateTracker.getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.seenIndicator());
    }

    @Override
    public void generalizeInstructionBlock(int evaluationCount)
    {
        stateTracker.getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.generalizationIndicator(evaluationCount));
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
            evaluation.variablesBefore = formatValueList(variablesBefore);
            evaluation.stackBefore = formatValueList(stackBefore);
            // TODO: add updatedEvaluationStack
        } else
        {
            stateTracker.getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                    StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.instructionTracker(
                            instruction.toString(), instructionOffset, null,
                            formatValueList(variablesBefore), formatValueList(stackBefore)
                    )
            );
        }
    }

    @Override
    public void afterInstructionEvaluation(BasicBranchUnit branchUnit, int instructionOffset, TracedVariables variables, TracedStack stack, InstructionOffsetValue branchTarget)
    {

    }

    @Override
    public void definitiveBranch(int instructionOffset, InstructionOffsetValue branchTargets)
    {

    }

    @Override
    public void registerAlternativeBranch(int index, int branchTargetCount, int instructionOffset, InstructionOffsetValue offsetValue, TracedVariables variables, TracedStack stack, int offset)
    {
        stateTracker.getLastCodeAttribute().lastEvaluationStack.add(new StateTracker.CodeAttributeTracker.InstructionBlock(
                formatValueList(variables), formatValueList(stack), offset
        ));
        stateTracker.getLastCodeAttribute().getLastBlockEvaluation().getLastEvaluation().updatedEvaluationStack =
                new ArrayList<>(stateTracker.getLastCodeAttribute().lastEvaluationStack);
    }

    // Subroutine:
    @Override
    public void generalizeSubroutine(int subroutineStart, int subroutineEnd)
    {
        throw new RuntimeException("NOT SUPPORTED");
    }

    @Override
    public void endSubroutine(int subroutineStart, int subroutineEnd)
    {
        throw new RuntimeException("NOT SUPPORTED");
    }

    @Override
    public void startSubroutine(int subroutineStart, int subroutineEnd)
    {
        throw new RuntimeException("NOT SUPPORTED");
    }
    
    // Access functions
    public String getJson() {
        for (StateTracker.CodeAttributeTracker tracker: stateTracker.codeAttributes) {
            tracker.lastEvaluationStack = null;
        }
        return gson.toJson(stateTracker);
    }

    public void printState()
    {
        System.out.println(getJson());
    }

    public void writeState() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("pe-state.json"));
            writer.write(getJson());

            writer.close();
        }
        catch (IOException ex) {
            // Do nothing
        }
    }
}
