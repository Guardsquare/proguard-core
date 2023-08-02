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
 * Track the evaluation of a single instruction block, starting at some offset in the code.
 */
class InstructionBlockEvaluationRecord implements JsonSerializable
{
    /**
     * List of instruction evaluation trackers.
     */
    @NotNull
    private final List<InstructionEvaluationRecord> evaluations;

    /**
     * Exception handler info. If present, this instructionBlock regards an exception handler.
     */
    private final ExceptionHandlerRecord exceptionHandlerInfo;

    /**
     * Variables found at the start of the block evaluation.
     */
    private List<String> startVariables;

    /**
     * Stack found at the start of the block evaluation.
     */
    private List<String> startStack;

    /**
     * Start instruction offset of this block evaluation.
     */
    private final int startOffset;

    /**
     * Current branch evaluation stack.
     */
    @NotNull
    private final List<BranchTargetRecord> branchEvaluationStack;

    public InstructionBlockEvaluationRecord(List<String> startVariables, List<String> startStack, int startOffset,
                                            ExceptionHandlerRecord exceptionHandlerInfo,
                                            @NotNull List<BranchTargetRecord> branchEvaluationStack)
    {
        this.evaluations = new ArrayList<>();
        this.startVariables = startVariables;
        this.startStack = startStack;
        this.startOffset = startOffset;
        this.exceptionHandlerInfo = exceptionHandlerInfo;
        this.branchEvaluationStack = branchEvaluationStack;
    }

    @Override
    public StringBuilder toJson(StringBuilder builder)
    {
        builder.append("{");
        JsonPrinter.toJson("startOffset", startOffset, builder).append(",");
        JsonPrinter.listToJson("evaluations", evaluations, builder).append(",");
        JsonPrinter.listToJson("branchEvaluationStack", branchEvaluationStack, builder);
        if (exceptionHandlerInfo != null) {
            builder.append(",");
            JsonPrinter.serializeJsonSerializable("exceptionHandlerInfo", exceptionHandlerInfo, builder);
        }
        if (startVariables != null)
        {
            builder.append(",");
            JsonPrinter.stringListToJson("startVariables", startVariables, builder);
        }
        if (startStack != null)
        {
            builder.append(",");
            JsonPrinter.stringListToJson("startStack", startStack, builder);
        }
        return builder.append("}");
    }

    public void setStartVariables(List<String> startVariables)
    {
        this.startVariables = startVariables;
    }

    public void setStartStack(List<String> startStack)
    {
        this.startStack = startStack;
    }

    public InstructionEvaluationRecord getLastInstructionEvaluation()
    {
        if (evaluations.isEmpty())
        {
            return null;
        }
        return evaluations.get(evaluations.size() - 1);
    }

    @NotNull
    public List<InstructionEvaluationRecord> getEvaluations()
    {
        return evaluations;
    }

    public ExceptionHandlerRecord getExceptionHandlerInfo()
    {
        return exceptionHandlerInfo;
    }

    public List<String> getStartVariables()
    {
        return startVariables;
    }

    public List<String> getStartStack()
    {
        return startStack;
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    @NotNull
    public List<BranchTargetRecord> getBranchEvaluationStack()
    {
        return branchEvaluationStack;
    }
}
