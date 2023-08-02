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

package proguard.evaluation.util.jsonprinter;

import org.jetbrains.annotations.NotNull;

class ErrorRecord implements JsonSerializable
{
    /**
     * Ths instruction offset of the instruction that caused the exception.
     */
    private final int instructionOffset;

    /**
     * The message of the exception.
     */
    @NotNull
    private final String message;

    public ErrorRecord(int instructionOffset, @NotNull String message)
    {
        this.instructionOffset = instructionOffset;
        this.message = message;
    }

    @Override
    public StringBuilder toJson(StringBuilder builder)
    {
        builder.append("{");
        JsonPrinter.toJson("instructionOffset", instructionOffset, builder).append(",");
        return JsonPrinter.toJson("message", message, builder).append("}");
    }

    public int getInstructionOffset()
    {
        return instructionOffset;
    }

    @NotNull
    public String getMessage()
    {
        return message;
    }
}
