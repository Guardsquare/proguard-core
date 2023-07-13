package proguard.evaluation.exception;

import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.exception.ProguardCoreException;
import proguard.util.CircularIntBuffer;

public class InstructionExceptionFormatter
{

    private final Logger logger;

    private final CircularIntBuffer offsetBuffer;

    private final byte[] code;

    private final Clazz clazz;

    private final Method method;

    public InstructionExceptionFormatter(Logger logger, CircularIntBuffer offsetBuffer, byte[] code, Clazz clazz, Method method)
    {
        this.logger = logger;
        this.offsetBuffer = offsetBuffer;
        this.code = code;
        this.clazz = clazz;
        this.method = method;
    }

    public void registerInstructionOffset(int offset) {
        offsetBuffer.push(offset);
    }

    public void printException(ProguardCoreException exception)
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
                .append("error[")
                .append(exception.getComponentErrorId())
                .append("]: ")
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
                .append(exception.getMessage())
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

        logger.error(messageBuilder.toString());

    }
}
