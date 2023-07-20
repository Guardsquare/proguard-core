package proguard.evaluation.stateTrackers.machinePrinter;

/**
 * DTO for exception handling info, when a blockEvaluation has this,
 * the block regard the evaluation of an exception handler
 */
class ExceptionHandlerRecord
{
    /**
     * Instruction offset from where the handler starts catching
     */
    private final int catchStartOffset;

    /**
     * Instruction offset from where the handler stops catching
     */
    private final int catchEndOffset;

    /**
     * Instruction offset of the exception handling code
     */
    private final int handlerStartOffset;

    /**
     * What type the handler catches
     */
    private final String catchType;

    public ExceptionHandlerRecord(int catchStartOffset, int catchEndOffset, int handlerStartOffset, String catchType)
    {
        this.catchStartOffset=catchStartOffset;
        this.catchEndOffset=catchEndOffset;
        this.handlerStartOffset=handlerStartOffset;
        this.catchType=catchType;
    }

    public int getCatchStartOffset()
    {
        return catchStartOffset;
    }

    public int getCatchEndOffset()
    {
        return catchEndOffset;
    }

    public int getHandlerStartOffset()
    {
        return handlerStartOffset;
    }

    public String getCatchType()
    {
        return catchType;
    }
}