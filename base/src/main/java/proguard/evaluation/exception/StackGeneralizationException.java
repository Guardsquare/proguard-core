package proguard.evaluation.exception;

import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.exception.ErrorId;
import proguard.exception.ProguardCoreException;

public class StackGeneralizationException extends ProguardCoreException
{
    private final TracedStack first;
    private final TracedStack second;

    public StackGeneralizationException(Throwable cause, TracedStack first, TracedStack second)
    {
        super(ErrorId.STACK_GENERALIZATION, cause, "Could not generalize stacks %s and %s because: \"%s\".",
                first.toString(), second.toString(), cause.getMessage());
        this.first = first;
        this.second = second;
    }

    public TracedStack getFirst()
    {
        return first;
    }

    public TracedStack getSecond()
    {
        return second;
    }
}
