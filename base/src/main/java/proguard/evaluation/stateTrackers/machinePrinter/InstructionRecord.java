package proguard.evaluation.stateTrackers.machinePrinter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * DTO to track a single instruction
 */
class InstructionRecord
{
    /**
     * The offset of the instruction
     */
    private final int offset;

    /**
     * String representation of the instruction
     */
    @NotNull
    private final String instruction;

    private List<String> finalStackBefore;

    private List<String> finalVariablesBefore;

    private List<Integer> finalTargetInstructions;

    private List<Integer> finalSourceInstructions;

    public InstructionRecord(int offset, @NotNull String instruction)
    {
        this.offset = offset;
        this.instruction = instruction;
    }

    public int getOffset()
    {
        return offset;
    }

    @NotNull
    public String getInstruction()
    {
        return instruction;
    }
}
