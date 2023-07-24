package proguard.evaluation.stateTrackers.machinePrinter;

import org.jetbrains.annotations.NotNull;

class ErrorRecord
{
    /**
     * Ths instruction offset of the instruction that caused the exception.
     */
    private final int instructionOffset;

    /**
     * The message of the exception.
     */
    @NotNull
    private final String message;

    public ErrorRecord(int instructionOffset, @NotNull String message)
    {
        this.instructionOffset = instructionOffset;
        this.message = message;
    }

    public int getInstructionOffset()
    {
        return instructionOffset;
    }

    @NotNull
    public String getMessage()
    {
        return message;
    }
}
