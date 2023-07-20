package proguard.evaluation.stateTrackers.machinePrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Track the evaluation of a single instruction block, starting at some offset in the code
 */
class InstructionBlockEvaluationRecord
{
    /**
     * List of instruction evaluation trackers.
     */
    public List<InstructionEvaluationRecord> evaluations = new ArrayList<>();

    /**
     * Exception handler info. If present, this instructionBlock regards an exception handler
     */
    public ExceptionHandlerRecord exceptionHandlerInfo;

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
     * Current branch evaluation stack
     */
    public List<BranchTargetRecord> branchEvaluationStack = new ArrayList<>();

    public InstructionBlockEvaluationRecord(List<String> startVariables, List<String> startStack, int startOffset)
    {
        this.startVariables=startVariables;
        this.startStack=startStack;
        this.startOffset=startOffset;
    }

    public InstructionEvaluationRecord getLastEvaluation()
    {
        if (evaluations.isEmpty())
        {
            return null;
        }
        return evaluations.get(evaluations.size() - 1);
    }
}
