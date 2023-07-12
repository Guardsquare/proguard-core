package proguard.evaluation.exception;

import proguard.evaluation.value.TypedReferenceValue;
import proguard.evaluation.value.Value;

public class ArrayStoreTypeException extends ArrayInstructionException
{
    private final TypedReferenceValue array;

    private final Value value;

    public ArrayStoreTypeException(TypedReferenceValue array, Value value)
    {
        super("Array of type \""+array.getType()+"\" can not store value \""+value+"\"", null);
        this.array = array;
        this.value = value;
    }
}
