package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.evaluation.Stack;
import proguard.util.CircularBuffer;

public class StackInstructionEvaluationException extends InstructionEvaluationException {
    public StackInstructionEvaluationException(String message, String note) {
        super(message, note);
    }

    public String getFormattedMessage(Clazz clazz, Method method, CircularBuffer<Integer> offsetBuffer, byte[] code, Stack stack) {
        return getFormattedMessage(clazz, method, offsetBuffer, code, "Stack before erroneous instruction: " + stack.toString());
    }
}
