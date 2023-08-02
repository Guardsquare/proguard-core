/*
 * ProGuardCORE -- library to process Java bytecode.
 *
 * Copyright (c) 2002-2023 Guardsquare NV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package proguard.exception;

import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.exception.ProguardCoreException;
import proguard.util.CircularIntBuffer;

/**
 * This class is used to format an exception with the previous instructions.
 * It is used by the {@link proguard.evaluation.PartialEvaluator} and {@link proguard.classfile.attribute.visitor.StackSizeComputer} to print the
 * erroneous instruction and any previous bytecode instructions and the next one to give some context.
 */
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

    public void registerInstructionOffset(int offset)
    {
        offsetBuffer.push(offset);
    }

    public void printException(ProguardCoreException exception)
    {
        printException(exception, null, null);
    }

    public void printException(ProguardCoreException exception, TracedVariables variables, TracedStack stack)
    {
        final String ANSI_RESET = "\u001B[0m";
        final String ANSI_BOLD = "\u001B[1m";
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_CYAN = "\u001B[34m";

        Instruction erroreousInstruction = InstructionFactory.create(code, offsetBuffer.peek());
        String errorInstructionString = erroreousInstruction.toString();

        // The error code and class of the exception
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder
                .append(ANSI_RED)
                .append(ANSI_BOLD)
                .append("error[")
                .append(exception.getComponentErrorId())
                .append("]: ")
                .append(ANSI_RESET)
                .append(exception.getClass().getName())
                .append("\n");

        // Clazz and Method of the erroneous instruction
        if (clazz != null && method != null)
        {
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
        for (int i = offsetBuffer.size() - 1; i > 0; i--)
        {
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
        if (nextOffset < code.length)
        {
            messageBuilder
                    .append(ANSI_CYAN)
                    .append(nextOffset)
                    .append(" |     ")
                    .append(ANSI_RESET)
                    .append(InstructionFactory.create(code, nextOffset))
                    .append("\n");
        }

        // Print stack and variables
        if (variables != null) {
            messageBuilder
                    .append("Variables: ")
                    .append(variables)
                    .append("\n");
        }

        if (stack != null)
        {
            messageBuilder
                    .append("Stack: ")
                    .append(stack)
                    .append("\n");
        }

        logger.error(messageBuilder.toString());

    }
}
