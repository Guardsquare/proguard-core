package proguard.evaluation.stateTrackers.machinePrinter;

import org.jetbrains.annotations.NotNull;

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
    @NotNull
    private final List<InstructionEvaluationRecord> evaluations;

    /**
     * Exception handler info. If present, this instructionBlock regards an exception handler
     */
    private final ExceptionHandlerRecord exceptionHandlerInfo;

    /**
     * Variables found at the start of the block evaluation.
     */
    private List<String> startVariables;

    /**
     * Stack found at the start of the block evaluation.
     */
    private List<String> startStack;

    /**
     * Start instruction offset of this block evaluation.
     */
    private final int startOffset;

    /**
     * Current branch evaluation stack
     */
    @NotNull
    private final List<BranchTargetRecord> branchEvaluationStack;

    public InstructionBlockEvaluationRecord(List<String> startVariables, List<String> startStack, int startOffset,
                                            ExceptionHandlerRecord exceptionHandlerInfo,
                                            @NotNull List<BranchTargetRecord> branchEvaluationStack)
    {
        this.evaluations = new ArrayList<>();
        this.startVariables = startVariables;
        this.startStack = startStack;
        this.startOffset = startOffset;
        this.exceptionHandlerInfo = exceptionHandlerInfo;
        this.branchEvaluationStack = branchEvaluationStack;
    }

    public void setStartVariables(List<String> startVariables)
    {
        this.startVariables = startVariables;
    }

    public void setStartStack(List<String> startStack)
    {
        this.startStack = startStack;
    }

    public InstructionEvaluationRecord getLastInstructionEvaluation()
    {
        if (evaluations.isEmpty())
        {
            return null;
        }
        return evaluations.get(evaluations.size() - 1);
    }

    @NotNull
    public List<InstructionEvaluationRecord> getEvaluations()
    {
        return evaluations;
    }

    public ExceptionHandlerRecord getExceptionHandlerInfo()
    {
        return exceptionHandlerInfo;
    }

    public List<String> getStartVariables()
    {
        return startVariables;
    }

    public List<String> getStartStack()
    {
        return startStack;
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    @NotNull
    public List<BranchTargetRecord> getBranchEvaluationStack()
    {
        return branchEvaluationStack;
    }
}
