package proguard.evaluation.exception;

import proguard.evaluation.TracedVariables;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

public class VariablesGeneralizationException extends ProguardCoreException
{
    private final TracedVariables first;
    private final TracedVariables second;

    public VariablesGeneralizationException(Throwable cause, TracedVariables first, TracedVariables second)
    {
        super(ErrorId.VARIABLE_GENERALIZATION, cause, "Could not generalize variables %s and %s because: \"%s\".",
                first.toString(), second.toString(), cause.getMessage());
        this.first = first;
        this.second = second;
    }

    public TracedVariables getFirst()
    {
        return first;
    }

    public TracedVariables getSecond()
    {
        return second;
    }
}
