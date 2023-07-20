package proguard.evaluation.stateTrackers.machinePrinter;

/**
 * DTO to track a single instruction
 */
class InstructionRecord
{
    /**
     * The offset of the instruction
     */
    public int offset;

    /**
     * String representation of the instruction
     */
    public String instruction;

    public InstructionRecord(int offset, String instruction)
    {
        this.offset=offset;
        this.instruction=instruction;
    }
}
