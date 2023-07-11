package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.PartialEvaluator;
import proguard.exception.ProguardCoreException;
import proguard.util.CircularIntBuffer;

import java.util.Collections;

/**
 * Represents an exception when the `PartialEvaluator` encounters a semantically incorrect java bytecode instruction.
 *
 * @see PartialEvaluator
 */
public class InstructionEvaluationException extends ProguardCoreException
{
    public InstructionEvaluationException(String genericMessage)
    {
        super(genericMessage, 4, Collections.emptyList());
    }

    protected String getFormattedMessage(Clazz clazz, Method method, CircularIntBuffer offsetBuffer, byte[] code, String locationDescription)
    {
        final String ANSI_RESET = "\u001B[0m";
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_CYAN = "\u001B[34m";

        Instruction erroreousInstruction = InstructionFactory.create(code, offsetBuffer.peek());
        String errorInstructionString = erroreousInstruction.toString();

        // The class of the error.
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder
                .append(ANSI_RED)
                .append("error: ")
                .append(ANSI_RESET)
                .append(getClass().getName())
                .append("\n");

        // Clazz and Method of the erroneous instruction
        if (clazz != null && method != null) {
            messageBuilder
                    .append(ANSI_CYAN)
                    .append("  --> ")
                    .append(ANSI_RESET)
                    .append(clazz.getName())
                    .append(" : ")
                    .append(method.getName(clazz))
                    .append(method.getDescriptor(clazz))
                    .append(" at ")
                    .append(errorInstructionString)
                    .append("\n");
        }

        // print the previous instructions
        for (int i = offsetBuffer.size() - 1; i > 0; i--) {
            Instruction prevInstruction = InstructionFactory.create(code, offsetBuffer.peek(i));
            int offset = offsetBuffer.peek(i);
            messageBuilder
                    .append(ANSI_CYAN)
                    .append(offset)
                    .append(" |     ")
                    .append(ANSI_RESET)
                    .append(prevInstruction)
                    .append("\n");
        }
        // print the erroneous instruction
        int offset = offsetBuffer.peek();
        messageBuilder
                .append(ANSI_CYAN)
                .append(offset)
                .append(" |")
                .append(ANSI_RESET)
                .append("     ")
                .append(errorInstructionString)
                .append("\n");

        String indicators = new String(new char[errorInstructionString.length()]).replace('\0', '^');
        messageBuilder
                .append("  ")
                .append(ANSI_CYAN)
                .append("|")
                .append(ANSI_RESET)
                .append("     ")
                .append(ANSI_RED)
                .append(indicators)
                .append(" ")
                .append(getMessage())
                .append(ANSI_RESET)
                .append("\n");

        int nextOffset = offsetBuffer.peek() + erroreousInstruction.length(offsetBuffer.peek());
        if (nextOffset < code.length) {
            messageBuilder
                    .append(ANSI_CYAN)
                    .append(nextOffset)
                    .append(" |     ")
                    .append(ANSI_RESET)
                    .append(InstructionFactory.create(code, nextOffset))
                    .append("\n");
        }

        if (locationDescription != null) {
            messageBuilder.append(locationDescription).append("\n");
        }

        return messageBuilder.toString();
    }

    public String getFormattedMessage(Clazz clazz, Method method, CircularIntBuffer offsetBuffer, byte[] code)
    {
        return getFormattedMessage(clazz, method, offsetBuffer, code, null);
    }
}
