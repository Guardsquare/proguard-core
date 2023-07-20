package proguard.evaluation.stateTrackers.machinePrinter;

class ErrorRecord
{
    /**
     * Ths instruction offset of the instruction that caused the exception.
     */
    public int instructionOffset;

    /**
     * The message of the exception.
     */
    public String message;

    public ErrorRecord(int instructionOffset, String message)
    {
        this.instructionOffset=instructionOffset;
        this.message=message;
    }
}