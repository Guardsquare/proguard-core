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
    public int catchStartOffset;

    /**
     * Instruction offset from where the handler stops catching
     */
    public int catchEndOffset;

    /**
     * Instruction offset of the exception handling code
     */
    public int handlerStartOffset;

    /**
     * What type the handler catches
     */
    public String catchType;

    public ExceptionHandlerRecord(int catchStartOffset, int catchEndOffset, int handlerStartOffset, String catchType)
    {
        this.catchStartOffset=catchStartOffset;
        this.catchEndOffset=catchEndOffset;
        this.handlerStartOffset=handlerStartOffset;
        this.catchType=catchType;
    }
}