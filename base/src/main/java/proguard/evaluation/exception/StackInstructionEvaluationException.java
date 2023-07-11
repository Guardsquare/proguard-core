package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.Stack;
import proguard.util.CircularIntBuffer;

public class StackInstructionEvaluationException extends InstructionEvaluationException
{
    public StackInstructionEvaluationException(String message)
    {
        super(message);
    }

    public String getFormattedMessage(Clazz clazz, Method method, CircularIntBuffer offsetBuffer, byte[] code, Stack stack)
    {
        return getFormattedMessage(clazz, method, offsetBuffer, code, "Stack before erroneous instruction: " + stack.toString());
    }
}
