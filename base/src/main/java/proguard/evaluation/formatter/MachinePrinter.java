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
import proguard.evaluation.value.Value;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/**
 * Capable of printing machine-readable output (JSON) xp
 * {
 *      "codeAttribute": {
 *          clazz
 *          method
 *          instructions: {offset, instruction}[],
 *          parameters: String(Value)[]
 *          blockEvaluations: {
 *              startOffset,
 *              startVariables,
 *              startStack,
 *              exceptionInfo?: { startOffset, endOffset, catchType (just string name) }
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
    /**
     * Track the state of a partial evaluator instance
     */
    static class StateTracker
    {
        /**
         * Track the evaluation of a single code attribute (one call to visitCode attribute)
         */
        static class CodeAttributeTracker
        {
            /**
             * Track a single instruction block. Used for tracking the instructionBlock stack generated
             * when using branches
             */
            static class InstructionBlock
            {
                public List<String> variables;
                public List<String> stack;
                public transient int startOffset;

                public InstructionBlock(List<String> variables, List<String> stack, int startOffset)
                {
                    this.variables=variables;
                    this.stack=stack;
                    this.startOffset=startOffset;
                }
            }

            /**
             * DTO for exception handling info, when a blockEvaluation has this,
             * the block regard the evaluation of an exception handler
             */
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

            /**
             * DTO to track a single instruction
             */
            static class InstructionTracker {
                public int offset;
                public String instruction;

                public InstructionTracker(int offset, String instruction)
                {
                    this.offset=offset;
                    this.instruction=instruction;
                }
            }

            /**
             * Track the evaluation of a single instruction block, starting at some offset in the code
             */
            static class BlockEvaluationTracker {
                /**
                 * Track information about the evaluation of a single instruction.
                 */
                static class InstructionEvaluationTracker
                {
                    /**
                     * Has the instrcution been seen in a given context before.
                     * When true, the instrcutionBlock evaluation comes to an end
                     */
                    public Boolean isSeenBefore;
                    /**
                     * Whether the instruction has been seen a lot, if true, start generalizing the values
                     */
                    public Boolean isGeneralization;

                    /**
                     * If we generalized, we remind how much times you saw the instruction.
                     */
                    public Integer timesSeen;

                    /**
                     * String representation of an instruction.
                     */
                    public String instruction;

                    /**
                     * Offset of the instruction within the code
                     */
                    public Integer instructionOffset;

                    /**
                     * Current stack of instruction blocks that need to be evaluated, used for branches
                     */
                    public List<InstructionBlock> updatedEvaluationStack;

                    /**
                     * Content of the variables before the instruction.
                     */
                    public List<String> variablesBefore;

                    /**
                     * Content of the stack before the instruction.
                     */
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

                /**
                 * List of instruction evaluation trackers.
                 */
                public List<InstructionEvaluationTracker> evaluations = new ArrayList<>();

                /**
                 * Exception handler info. If present, this instructionBlock regards a exception handler
                 */
                public ExceptionHandlerInfo exceptionHandlerInfo;

                /**
                 * Variables found at the start of the block evaluation.
                 */
                public List<String> startVariables;

                /**
                 * Stack found at the start of the block evaluation.
                 */
                public List<String> startStack;

                /**
                 * Start instruction offset of this block evaluation.
                 */
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

            /**
             * Clazz this code attribute is a part of.
             */
            public String clazz;

            /**
             * Method this code attribute is from.
             */
            public String method;

            /**
             * List of instruction from this code attribute.
             */
            public List<InstructionTracker> instructions = new ArrayList<>();

            /**
             * List of parameters given to the code attribute.
             */
            public List<String> parameters;

            /**
             * List of block evaluations that happened on this code attribute.
             */
            public List<BlockEvaluationTracker> blockEvaluations = new ArrayList<>();

            /**
             * List of what the last Evaluation block stack was (used for branching) transient since we want to track it here
             * -> move higher up
             */
            public transient List<InstructionBlock> lastEvaluationStack = new ArrayList<>();

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
            /**
             * Clazz this code attribute is a part of.
             */
            public String clazz;

            /**
             * Method this code attribute is from.
             */
            public String method;

            /**
             * Ths instruction offset of the instruction that caused the exception.
             */
            public int instructionOffset;

            /**
             * The message of the exception.
             */
            public String message;

            /**
             * Idk, me no use yet
             */
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

    private final Gson gson;
    private final StateTracker stateTracker = new StateTracker();
    private final Deque<List<StateTracker.CodeAttributeTracker.BlockEvaluationTracker>> subRoutineTrackers;

    public MachinePrinter()
    {
        subRoutineTrackers = new ArrayDeque<>();
        gson=new GsonBuilder().setPrettyPrinting().create();
    }

    public List<StateTracker.CodeAttributeTracker.BlockEvaluationTracker> getLastBlockEvaluationTracker() {
        if (subRoutineTrackers.isEmpty()) {
            return null;
        }
        return subRoutineTrackers.peekLast();
    }

    private List<String> formatValueList(Variables variables) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            Value val = variables.getValue(i);
            res.add(val == null ? "empty" : val.toString());
        }
        return res;
    }

    private List<String> formatValueList(Stack stack) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < stack.size(); i++) {
            Value val = stack.getBottom(i);
            res.add(val == null ? "empty" : val.toString());
        }
        return res;
    }

    // Code attribute level:
    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        // Register the current code attribute
        stateTracker.codeAttributes.add(new StateTracker.CodeAttributeTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz), formatValueList(parameters)
        ));

        // Clear the subroutine recursion tracker
        subRoutineTrackers.clear();
        subRoutineTrackers.add(stateTracker.getLastCodeAttribute().blockEvaluations);

        // Read out all instructions in this codeAttribute
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
        // Register an exception in the top level stateTracker
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

    }

    @Override
    public void registerExceptionHandler(int startPC, int endPC, int handlerPC, ExceptionInfo info, Clazz clazz)
    {
        // Register an exception handler being evaluated
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
        // Start of a single block evaluation

        // If we have an InstructionBlockEvaluation stack, pop one.
        if (!stateTracker.getLastCodeAttribute().lastEvaluationStack.isEmpty()) {
            stateTracker.getLastCodeAttribute().lastEvaluationStack.remove(getLastStateTracker().getLastCodeAttribute().lastEvaluationStack.size() - 1);
        }
        // If the last evaluation was handling an exception, this one is also
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker blockTracker = null;
        StateTracker.CodeAttributeTracker.ExceptionHandlerInfo exInfo = null;
        if (getLastStateTracker().getLastCodeAttribute() != null && getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation() != null)
        {
            blockTracker = getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation();
            exInfo = blockTracker.exceptionHandlerInfo;
        }
        if (blockTracker != null && exInfo != null && blockTracker.evaluations.isEmpty()) {
            blockTracker.startVariables = formatValueList(variables);
            blockTracker.startStack = formatValueList(stack);
        }
        else
        {
            getLastStateTracker().getLastCodeAttribute().blockEvaluations.add(
                    new StateTracker.CodeAttributeTracker.BlockEvaluationTracker(
                            formatValueList(variables), formatValueList(stack), startOffset
                    ));
            getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().exceptionHandlerInfo = exInfo;
        }
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
        getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.seenIndicator());
    }

    @Override
    public void generalizeInstructionBlock(int evaluationCount)
    {
        getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.generalizationIndicator(evaluationCount));
    }

    @Override
    public void startInstructionEvaluation(Instruction instruction, Clazz clazz, int instructionOffset,
                                           TracedVariables variablesBefore, TracedStack stackBefore)
    {
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker evaluation =
                getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().getLastEvaluation();
        if (evaluation != null && evaluation.isGeneralization != null && evaluation.isGeneralization) {
            evaluation.instruction = instruction.toString();
            evaluation.instructionOffset = instructionOffset;
            evaluation.variablesBefore = formatValueList(variablesBefore);
            evaluation.stackBefore = formatValueList(stackBefore);
            // TODO: add updatedEvaluationStack
        } else
        {
            getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().evaluations.add(
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
        getLastStateTracker().getLastCodeAttribute().lastEvaluationStack.add(new StateTracker.CodeAttributeTracker.InstructionBlock(
                formatValueList(variables), formatValueList(stack), offset
        ));
        getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().getLastEvaluation().updatedEvaluationStack =
                new ArrayList<>(getLastStateTracker().getLastCodeAttribute().lastEvaluationStack);
    }


    // Subroutine:
    @Override
    public void startSubroutine(int subroutineStart, int subroutineEnd)
    {
        StateTracker recursiveTracker = new StateTracker();
        getLastStateTracker().getLastCodeAttribute().getLastBlockEvaluation().getLastEvaluation().subroutineTracker = recursiveTracker;
        stateTrackers.offer(recursiveTracker);
    }

    @Override
    public void generalizeSubroutine(int subroutineStart, int subroutineEnd)
    {
        throw new RuntimeException("NOT SUPPORTED");
    }

    @Override
    public void endSubroutine(int subroutineStart, int subroutineEnd)
    {
        stateTrackers.pop();
    }


    // Access functions
    public String getJson() {
        for (StateTracker.CodeAttributeTracker tracker: getLastStateTracker().codeAttributes) {
            tracker.lastEvaluationStack = null;
        }
        return gson.toJson(getLastStateTracker());
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
