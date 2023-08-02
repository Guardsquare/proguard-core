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

/**
 * DTO for exception handling info, when a blockEvaluation has this,
 * the block regard the evaluation of an exception handler.
 */
class ExceptionHandlerRecord implements JsonSerializable
{
    /**
     * Instruction offset from where the handler starts catching.
     */
    private final int catchStartOffset;

    /**
     * Instruction offset from where the handler stops catching.
     */
    private final int catchEndOffset;

    /**
     * Instruction offset of the exception handling code.
     */
    private final int handlerStartOffset;

    /**
     * What type the handler catches.
     */
    @NotNull
    private final String catchType;

    public ExceptionHandlerRecord(int catchStartOffset, int catchEndOffset, int handlerStartOffset, @NotNull String catchType)
    {
        this.catchStartOffset = catchStartOffset;
        this.catchEndOffset = catchEndOffset;
        this.handlerStartOffset = handlerStartOffset;
        this.catchType = catchType;
    }

    @Override
    public StringBuilder toJson(StringBuilder builder)
    {
        builder.append("{");
        JsonPrinter.toJson("catchStartOffset", catchStartOffset, builder).append(",");
        JsonPrinter.toJson("catchEndOffset", catchEndOffset, builder).append(",");
        JsonPrinter.toJson("handlerStartOffset", handlerStartOffset, builder).append(",");
        return JsonPrinter.toJson("catchType", catchType, builder).append("}");
    }

    public int getCatchStartOffset()
    {
        return catchStartOffset;
    }

    public int getCatchEndOffset()
    {
        return catchEndOffset;
    }

    public int getHandlerStartOffset()
    {
        return handlerStartOffset;
    }

    @NotNull
    public String getCatchType()
    {
        return catchType;
    }
}
