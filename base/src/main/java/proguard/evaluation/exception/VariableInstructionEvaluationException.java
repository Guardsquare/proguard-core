package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.Variables;
import proguard.util.CircularBuffer;

public class VariableInstructionEvaluationException extends InstructionEvaluationException
{
    public VariableInstructionEvaluationException(String message, String note)
    {
        super(message, note);
    }

    public String getFormattedMessage(Clazz clazz, Method method, CircularBuffer<Integer> offsetBuffer, byte[] code, Variables variables)
    {
        return getFormattedMessage(clazz, method, offsetBuffer, code, "Variables: " + variables.toString());
    }
}
