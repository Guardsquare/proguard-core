package proguard.evaluation.stateTrackers;

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
import java.util.Arrays;
import java.util.Deque;
import java.util.List;


/**
 * Capable of printing machine-readable output (JSON) xp
 * {
 *      "codeAttribute": {
 *          clazz
 *          method
 *          instructions: {offset, instruction}[],
 *          parameters: String(Value)[],
 *          error?: {
 *              clazz,
 *              method,
 *              message,
 *              stacktrace,
 *          }
 *          blockEvaluations: {
 *              startOffset,
 *              startVariables,
 *              startStack,
 *              blockEvaluationStack
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

                public ErrorTracker(String clazz, String method, int instructionOffset, String message)
                {
                    this.clazz=clazz;
                    this.method=method;
                    this.instructionOffset=instructionOffset;
                    this.message=message;
                }
            }

            /**
             * Track a single instruction block. Used for tracking the instructionBlock stack generated
             * when using branches
             */
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

                    public List<BlockEvaluationTracker> jsrBlockEvalTracker;

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

                /**
                 * Current block evaluation stack. Not encoded if not changed.
                 */
                public List<InstructionBlock> blockEvaluationStack = new ArrayList<>();

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

            public ErrorTracker error;

            /**
             * List of block evaluations that happened on this code attribute.
             */
            public List<BlockEvaluationTracker> blockEvaluations = new ArrayList<>();

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

        public final List<CodeAttributeTracker> codeAttributes=new ArrayList<>();

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

    public StateTracker.CodeAttributeTracker.BlockEvaluationTracker getLastBlockEvaluation() {
        if (subRoutineTrackers.isEmpty()) {
            return null;
        }
        List<StateTracker.CodeAttributeTracker.BlockEvaluationTracker> list = subRoutineTrackers.peekLast();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
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
        stateTracker.getLastCodeAttribute().error = new StateTracker.CodeAttributeTracker.ErrorTracker(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz),
                getLastBlockEvaluation().getLastEvaluation().instructionOffset
                , cause.getMessage()
        );
    }


    // Exceptions
    @Override
    public void startExceptionHandlingForBlock(Clazz clazz, Method method, int startOffset, int endOffset)
    {

    }

    @Override
    public void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        // Register an exception handler being evaluated
        stateTracker.getLastCodeAttribute().blockEvaluations.add(new StateTracker.CodeAttributeTracker.BlockEvaluationTracker(
                null, null, info.u2handlerPC
        ));

        // No need to copy branch stack

        ClassConstant constant =(ClassConstant) ((ProgramClass) clazz).getConstant(info.u2catchType);
       getLastBlockEvaluation().exceptionHandlerInfo =
                new StateTracker.CodeAttributeTracker.ExceptionHandlerInfo(
                        startPC, endPC, constant == null ? "java/lang/Throwable" : constant.getName(clazz)
                );
    }

    @Override
    public void registerUnusedExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
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
                                      TracedVariables startVariables, TracedStack startStack, int startOffset)
    {
        // Start of a single block evaluation

        // If the last evaluation was handling an exception, this one is also
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker blockTracker = null;
        StateTracker.CodeAttributeTracker.ExceptionHandlerInfo exInfo = null;
        if (getLastBlockEvaluation() != null)
        {
            blockTracker = getLastBlockEvaluation();
            exInfo = blockTracker.exceptionHandlerInfo;
        }

        // If the last blockTracker is not initialized, it is one created by registerException; initialize it
        if (blockTracker != null && exInfo != null && blockTracker.evaluations.isEmpty()) {
            blockTracker.startVariables = formatValueList(startVariables);
            blockTracker.startStack = formatValueList(startStack);
        }
        else
        {
            // Copy the last instruction block
            StateTracker.CodeAttributeTracker.BlockEvaluationTracker lastBlock =
                    getLastBlockEvaluation();
            List<StateTracker.CodeAttributeTracker.InstructionBlock> branchStack = new ArrayList<>();
            if (lastBlock != null) {
                if (lastBlock.getLastEvaluation() != null && lastBlock.getLastEvaluation().updatedEvaluationStack != null) {
                    branchStack = new ArrayList<>(lastBlock.getLastEvaluation().updatedEvaluationStack);
                } else {
                    branchStack = new ArrayList<>(lastBlock.blockEvaluationStack);
                }
            }
            if (!branchStack.isEmpty())
            {
                branchStack.remove(branchStack.size()-1);
            }

            getLastBlockEvaluationTracker().add(
                    new StateTracker.CodeAttributeTracker.BlockEvaluationTracker(
                            formatValueList(startVariables), formatValueList(startStack), startOffset
                    ));
            getLastBlockEvaluation().exceptionHandlerInfo = exInfo;
            getLastBlockEvaluation().blockEvaluationStack = branchStack;
        }
    }

    @Override
    public void startBranchCodeBlockEvaluation(List<PartialEvaluator.InstructionBlockDTO> branchStack)
    {

    }

    @Override
    public void instructionBlockDone(Clazz clazz,
                                     Method method,
                                     CodeAttribute codeAttribute,
                                     TracedVariables startVariables,
                                     TracedStack startStack,
                                     int startOffset)
    {

    }


    // Instruction level:
    @Override
    public void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesBefore, TracedStack stackBefore)
    {
        getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.seenIndicator());
    }

    @Override
    public void generalizeInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        getLastBlockEvaluation().evaluations.add(
                StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.generalizationIndicator(evaluationCount));
    }

    @Override
    public void startInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                           TracedVariables variablesBefore, TracedStack stackBefore)
    {
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker prevEval =
               getLastBlockEvaluation().getLastEvaluation();
        if (prevEval != null && prevEval.isGeneralization != null && prevEval.isGeneralization) {
            prevEval.instruction = instruction.toString();
            prevEval.instructionOffset = instructionOffset;
            prevEval.variablesBefore = formatValueList(variablesBefore);
            prevEval.stackBefore = formatValueList(stackBefore);
        } else
        {
            getLastBlockEvaluation().evaluations.add(
                    StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker.instructionTracker(
                            instruction.toString(), instructionOffset, null,
                            formatValueList(variablesBefore), formatValueList(stackBefore)
                    )
            );
        }
    }

    @Override
    public void afterInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesAfter, TracedStack stackAfter, BasicBranchUnit branchUnit, InstructionOffsetValue branchTarget)
    {

    }

    @Override
    public void definitiveBranch(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesAfter, TracedStack stackAfter, InstructionOffsetValue branchTargets)
    {

    }

    @Override
    public void registerAlternativeBranch(Clazz clazz, Method method, int fromInstructionOffset, Instruction fromInstruction, TracedVariables variablesAfter, TracedStack stackAfter, int branchIndex, int branchTargetCount, int offset)
    {
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker blockEval = getLastBlockEvaluation();
        StateTracker.CodeAttributeTracker.BlockEvaluationTracker.InstructionEvaluationTracker lastEval = blockEval.getLastEvaluation();

        if (lastEval.updatedEvaluationStack == null) {
            lastEval.updatedEvaluationStack = new ArrayList<>(blockEval.blockEvaluationStack);
        }
        lastEval.updatedEvaluationStack.add(new StateTracker.CodeAttributeTracker.InstructionBlock(
            formatValueList(variablesAfter), formatValueList(stackAfter), offset
        ));
    }


    // Subroutine:
    @Override
    public void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd)
    {
        getLastBlockEvaluation().getLastEvaluation().jsrBlockEvalTracker = new ArrayList<>();
        subRoutineTrackers.offer(getLastBlockEvaluation().getLastEvaluation().jsrBlockEvalTracker);
    }

    @Override
    public void generalizeSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd)
    {
    }

    @Override
    public void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter, int subroutineStart, int subroutineEnd)
    {
        subRoutineTrackers.pop();
    }


    // Access functions
    public String getJson() {
        return gson.toJson(stateTracker);
    }

    public void printState()
    {
        System.out.println(getJson());
    }

    public void writeState(String fileName) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(getJson());

            writer.close();
        }
        catch (IOException ex) {
            // Do nothing
        }
    }
}
