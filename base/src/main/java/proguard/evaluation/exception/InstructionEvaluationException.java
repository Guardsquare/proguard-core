package proguard.evaluation.exception;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.Stack;
import proguard.evaluation.Variables;
import proguard.exception.ProguardCoreException;
import proguard.util.CircularBuffer;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * Represents an exception when the `PartialEvaluator` encounters a semantically incorect java bytecode instruction.
 * A note can be passed to provide some extra information or possible hints to solve the issue.
 *
 * @see PartialEvaluator
 */
public class InstructionEvaluationException extends ProguardCoreException {
    private final String note;

    public InstructionEvaluationException(String genericMessage, String genericNote) {
        super(genericMessage, 4, Collections.emptyList());
        this.note = genericNote;
    }

    protected String getFormattedMessage(Clazz clazz, Method method, CircularBuffer<Integer> offsetBuffer, byte[] code, String locationDescription) {
        final String ANSI_RESET = "\u001B[0m";
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_CYAN = "\u001B[34m";

        Instruction erroreousInstruction = InstructionFactory.create(code, offsetBuffer.peek());
        String errorInstructionString = erroreousInstruction.toString();


        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder
                .append(ANSI_RED)
                .append("error: ")
                .append(ANSI_RESET)
                .append(getClass().getName())
                .append("\n");
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
        for (int i = 0; i < offsetBuffer.size() - 1; i++) {
            Instruction prevInstruction = InstructionFactory.create(code, offsetBuffer.elementAt(i));
            int offset = offsetBuffer.elementAt(i);
            messageBuilder
                    .append(ANSI_CYAN)
                    .append(offset)
                    .append(" |     ")
                    .append(ANSI_RESET)
                    .append(prevInstruction)
                    .append("\n");
        }
        // print the erroneous instruction
        int offset = offsetBuffer.lastElement();
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
        messageBuilder.append(note).append("\n");

        return messageBuilder.toString();
    }

    public String getFormattedMessage(Clazz clazz, Method method, CircularBuffer<Integer> offsetBuffer, byte[] code) {
        return getFormattedMessage(clazz, method, offsetBuffer, code, null);
    }

    public String getNote() {
        return note;
    }
}
