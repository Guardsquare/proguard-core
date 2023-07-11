package proguard.evaluation.exception;

import proguard.evaluation.value.Value;

public class ArrayInstructionOnWrongTypeException extends ArrayInstructionException
{
    protected final Value wrongValue;

    public ArrayInstructionOnWrongTypeException(Value wrongValue)
    {
        super("Invalid reference provided to arrayInstruction. Expected arrayReference but found: "+wrongValue.toString()+".", null);
        this.wrongValue = wrongValue;
    }
}
