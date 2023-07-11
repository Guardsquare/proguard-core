package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.Variables;
import proguard.util.CircularIntBuffer;

public class VariableInstructionEvaluationException extends InstructionEvaluationException
{
    public VariableInstructionEvaluationException(String message)
    {
        super(message);
    }

    public String getFormattedMessage(Clazz clazz, Method method, CircularIntBuffer offsetBuffer, byte[] code, Variables variables)
    {
        return getFormattedMessage(clazz, method, offsetBuffer, code, "Variables: " + variables.toString());
    }
}
