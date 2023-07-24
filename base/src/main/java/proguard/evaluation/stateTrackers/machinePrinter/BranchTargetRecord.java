package proguard.evaluation.stateTrackers.machinePrinter;

import java.util.List;

class BranchTargetRecord
{
    /**
     * Variables at the start of the block evaluation
     */
    private final List<String> startVariables;

    /**
     * Stack at the start of the block evaluation
     */
    private final List<String> startStack;

    /**
     * Instruction offset of the first instruction of the block
     */
    private final int startOffset;

    public BranchTargetRecord(List<String> variables, List<String> stack, int startOffset)
    {
        this.startVariables=variables;
        this.startStack=stack;
        this.startOffset=startOffset;
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
}
