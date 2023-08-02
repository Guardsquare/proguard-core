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

import java.util.ArrayList;
import java.util.List;

/**
 * Track the evaluation of a single code attribute (one call to visitCode attribute)
 */
class CodeAttributeRecord implements JsonSerializable
{
    /**
     * Clazz this code attribute is a part of.
     */
    @NotNull
    private final String clazz;

    /**
     * Method this code attribute is from.
     */
    @NotNull
    private final String method;

    /**
     * List of instruction from this code attribute.
     */
    @NotNull
    private final List<InstructionRecord> instructions;

    /**
     * List of parameters given to the code attribute.
     */
    @NotNull
    private final List<String> parameters;

    private ErrorRecord error;

    /**
     * List of block evaluations that happened on this code attribute.
     */
    @NotNull
    private final List<InstructionBlockEvaluationRecord> blockEvaluations = new ArrayList<>();

    public CodeAttributeRecord(@NotNull String clazz, @NotNull String method, @NotNull List<String> parameters,
                               @NotNull List<InstructionRecord> instructions)
    {
        this.clazz = clazz;
        this.method = method;
        this.parameters = parameters;
        this.instructions = instructions;
    }

    public StringBuilder toJson(StringBuilder builder) {
        builder.append("{");
        JsonPrinter.toJson("clazz", clazz, builder).append(",");
        JsonPrinter.toJson("method", method, builder).append(",");
        JsonPrinter.listToJson("instructions", instructions, builder).append(",");
        JsonPrinter.stringListToJson("parameters", parameters, builder).append(",");
        JsonPrinter.listToJson("blockEvaluations", blockEvaluations, builder);
        if (error != null) {
            builder.append(",");
            JsonPrinter.serializeJsonSerializable("error", error, builder);
        }
        return builder.append("}");
    }

    @NotNull
    public String getClazz()
    {
        return clazz;
    }

    @NotNull
    public String getMethod()
    {
        return method;
    }

    @NotNull
    public List<InstructionRecord> getInstructions()
    {
        return instructions;
    }

    @NotNull
    public List<String> getParameters()
    {
        return parameters;
    }

    public ErrorRecord getError()
    {
        return error;
    }

    @NotNull
    public List<InstructionBlockEvaluationRecord> getBlockEvaluations()
    {
        return blockEvaluations;
    }

    public void setError(ErrorRecord error)
    {
        this.error = error;
    }
}
