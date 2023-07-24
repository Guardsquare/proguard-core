package proguard.evaluation.stateTrackers.machinePrinter;

class ErrorRecord
{
    /**
     * Ths instruction offset of the instruction that caused the exception.
     */
    private final int instructionOffset;

    /**
     * The message of the exception.
     */
    private final String message;

    public ErrorRecord(int instructionOffset, String message)
    {
        this.instructionOffset=instructionOffset;
        this.message=message;
    }

    public int getInstructionOffset()
    {
        return instructionOffset;
    }

    public String getMessage()
    {
        return message;
    }
}
