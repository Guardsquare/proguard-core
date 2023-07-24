package proguard.evaluation.stateTrackers.machinePrinter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Track information about the evaluation of a single instruction.
 */
class InstructionEvaluationRecord
{
    /**
     * Has the instruction been seen in a given context before.
     * When true, the instructionBlock evaluation comes to an end
     */
    private final boolean skipEvaluation;

    /**
     * Whether the instruction has been seen a lot, if true, start generalizing the values
     */
    private final boolean isGeneralization;

    /**
     * If we generalized, we remind how much times you saw the instruction.
     */
    private final int evaluationCount;

    /**
     * String representation of an instruction.
     */
    @NotNull
    private final String instruction;

    /**
     * Offset of the instruction within the code
     */
    private final int instructionOffset;

    /**
     * Current stack of instruction blocks that need to be evaluated, used for branches,
     * only given when the instruction alters the branch evaluation stack
     */
    private List<BranchTargetRecord> updatedEvaluationStack;

    /**
     * Content of the variables before the instruction.
     */
    @NotNull
    private final List<String> variablesBefore;

    /**
     * Content of the stack before the instruction.
     */
    @NotNull
    private final List<String> stackBefore;

    private List<InstructionBlockEvaluationRecord> jsrBlockEvaluations;

    public InstructionEvaluationRecord(
            Boolean skipEvaluation, Boolean isGeneralization, Integer evaluationCount, @NotNull String instruction,
            Integer instructionOffset, @NotNull List<String> variablesBefore, @NotNull List<String> stackBefore)
    {
        this.skipEvaluation = skipEvaluation;
        this.isGeneralization = isGeneralization;
        this.evaluationCount = evaluationCount;
        this.instruction = instruction;
        this.instructionOffset = instructionOffset;
        this.variablesBefore = variablesBefore;
        this.stackBefore = stackBefore;
    }

    public void setUpdatedEvaluationStack(List<BranchTargetRecord> updatedEvaluationStack)
    {
        this.updatedEvaluationStack = updatedEvaluationStack;
    }

    public void setJsrBlockEvaluations(List<InstructionBlockEvaluationRecord> jsrBlockEvaluations)
    {
        this.jsrBlockEvaluations = jsrBlockEvaluations;
    }

    public boolean isSkipEvaluation()
    {
        return skipEvaluation;
    }

    public boolean isGeneralization()
    {
        return isGeneralization;
    }

    public int getEvaluationCount()
    {
        return evaluationCount;
    }

    @NotNull
    public String getInstruction()
    {
        return instruction;
    }

    public int getInstructionOffset()
    {
        return instructionOffset;
    }

    public List<BranchTargetRecord> getUpdatedEvaluationStack()
    {
        return updatedEvaluationStack;
    }

    @NotNull
    public List<String> getVariablesBefore()
    {
        return variablesBefore;
    }

    @NotNull
    public List<String> getStackBefore()
    {
        return stackBefore;
    }

    public List<InstructionBlockEvaluationRecord> getJsrBlockEvaluations()
    {
        return jsrBlockEvaluations;
    }
}
