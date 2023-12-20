package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.TracedStack;
import proguard.exception.ErrorId;

public class StackGeneralizationException extends PartialEvaluatorException
{
    private final TracedStack first;
    private final TracedStack second;

    @Deprecated
    public StackGeneralizationException(Throwable cause, TracedStack first, TracedStack second)
    {
        this(null, null, cause, first, second);
    }


    public StackGeneralizationException(Clazz clazz, Method method, Throwable cause, TracedStack first, TracedStack second)
    {
        super(ErrorId.STACK_GENERALIZATION, cause,clazz, method,  "Could not generalize stacks %s and %s because: \"%s\".",
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
