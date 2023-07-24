package proguard.evaluation.stateTrackers.machinePrinter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class BranchTargetRecord
{
    /**
     * Variables at the start of the block evaluation
     */
    @NotNull
    private final List<String> startVariables;

    /**
     * Stack at the start of the block evaluation
     */
    @NotNull
    private final List<String> startStack;

    /**
     * Instruction offset of the first instruction of the block
     */
    private final int startOffset;

    public BranchTargetRecord(@NotNull List<String> variables, @NotNull List<String> stack, int startOffset)
    {
        this.startVariables = variables;
        this.startStack = stack;
        this.startOffset = startOffset;
    }

    @NotNull
    public List<String> getStartVariables()
    {
        return startVariables;
    }

    @NotNull
    public List<String> getStartStack()
    {
        return startStack;
    }

    public int getStartOffset()
    {
        return startOffset;
    }
}
