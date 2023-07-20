package proguard.evaluation.stateTrackers.machinePrinter;

import java.util.List;

class BranchTargetRecord
{
    /**
     * Variables at the start of the block evaluation
     */
    public List<String> startVariables;

    /**
     * Stack at the start of the block evaluation
     */
    public List<String> startStack;

    /**
     * Instruction offset of teh first instruction of the block
     */
    public int startOffset;

    public BranchTargetRecord(List<String> variables, List<String> stack, int startOffset)
    {
        this.startVariables=variables;
        this.startStack=stack;
        this.startOffset=startOffset;
    }
}