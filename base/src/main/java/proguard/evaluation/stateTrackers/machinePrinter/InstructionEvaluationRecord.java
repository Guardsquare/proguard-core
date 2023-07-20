package proguard.evaluation.stateTrackers.machinePrinter;

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
    public Boolean skipEvaluation;
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
     * Current stack of instruction blocks that need to be evaluated, used for branches,
     * only given when the instruction alters the branch evaluation stack
     */
    public List<BranchTargetRecord> updatedEvaluationStack;

    /**
     * Content of the variables before the instruction.
     */
    public List<String> variablesBefore;

    /**
     * Content of the stack before the instruction.
     */
    public List<String> stackBefore;

    public List<InstructionBlockEvaluationRecord> jsrBlockEvaluations;

    public InstructionEvaluationRecord(
            Boolean skipEvaluation, Boolean isGeneralization, Integer timesSeen, String instruction,
            Integer instructionOffset, List<String> variablesBefore, List<String> stackBefore)
    {
        this.skipEvaluation = skipEvaluation;
        this.isGeneralization = isGeneralization;
        this.timesSeen = timesSeen;
        this.instruction = instruction;
        this.instructionOffset = instructionOffset;
        this.variablesBefore = variablesBefore;
        this.stackBefore = stackBefore;
    }
}