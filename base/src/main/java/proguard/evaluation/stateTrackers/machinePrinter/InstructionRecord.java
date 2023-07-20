package proguard.evaluation.stateTrackers.machinePrinter;

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
    private final String instruction;

    public InstructionRecord(int offset, String instruction)
    {
        this.offset = offset;
        this.instruction = instruction;
    }

    public int getOffset()
    {
        return offset;
    }

    public String getInstruction()
    {
        return instruction;
    }
}
