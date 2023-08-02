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

import java.util.List;

/**
 * Track information about the evaluation of a single instruction.
 */
class InstructionEvaluationRecord implements JsonSerializable
{
    /**
     * Has the instruction been seen in a given context before.
     * When true, the instructionBlock evaluation comes to an end.
     */
    private final boolean skipEvaluation;

    /**
     * Whether the instruction has been seen a lot, if true, start generalizing the values.
     */
    private final boolean isGeneralization;

    /**
     * If we generalized, we remind how much times you saw the instruction.
     */
    private final int evaluationCount;

    /**
     * String representation of an instruction.
     */
    @NotNull
    private final String instruction;

    /**
     * Offset of the instruction within the code.
     */
    private final int instructionOffset;

    /**
     * Current stack of instruction blocks that need to be evaluated, used for branches,
     * only given when the instruction alters the branch evaluation stack.
     */
    private List<BranchTargetRecord> updatedEvaluationStack;

    /**
     * Content of the variables before the instruction.
     */
    @NotNull
    private final List<String> variablesBefore;

    /**
     * Content of the stack before the instruction.
     */
    @NotNull
    private final List<String> stackBefore;

    private List<InstructionBlockEvaluationRecord> jsrBlockEvaluations;

    public InstructionEvaluationRecord(
            Boolean skipEvaluation, Boolean isGeneralization, Integer evaluationCount, @NotNull String instruction,
            Integer instructionOffset, @NotNull List<String> variablesBefore, @NotNull List<String> stackBefore)
    {
        this.skipEvaluation = skipEvaluation;
        this.isGeneralization = isGeneralization;
        this.evaluationCount = evaluationCount;
        this.instruction = instruction;
        this.instructionOffset = instructionOffset;
        this.variablesBefore = variablesBefore;
        this.stackBefore = stackBefore;
    }

    @Override
    public StringBuilder toJson(StringBuilder builder)
    {
        builder.append("{");
        JsonPrinter.toJson("skipEvaluation", skipEvaluation, builder).append(",");
        JsonPrinter.toJson("isGeneralization", isGeneralization, builder).append(",");
        JsonPrinter.toJson("evaluationCount", evaluationCount, builder).append(",");
        JsonPrinter.toJson("instruction", instruction, builder).append(",");
        JsonPrinter.toJson("instructionOffset", instructionOffset, builder).append(",");
        JsonPrinter.stringListToJson("variablesBefore", variablesBefore, builder).append(",");
        JsonPrinter.stringListToJson("stackBefore", stackBefore, builder);
        if (updatedEvaluationStack != null)
        {
            builder.append(",");
            JsonPrinter.listToJson("updatedEvaluationStack", updatedEvaluationStack, builder);
        }
        if (jsrBlockEvaluations != null)
        {
            builder.append(",");
            JsonPrinter.listToJson("jsrBlockEvaluations", jsrBlockEvaluations, builder);
        }
        return builder.append("}");
    }

    public void setUpdatedEvaluationStack(List<BranchTargetRecord> updatedEvaluationStack)
    {
        this.updatedEvaluationStack = updatedEvaluationStack;
    }

    public void setJsrBlockEvaluations(List<InstructionBlockEvaluationRecord> jsrBlockEvaluations)
    {
        this.jsrBlockEvaluations = jsrBlockEvaluations;
    }

    public boolean isSkipEvaluation()
    {
        return skipEvaluation;
    }

    public boolean isGeneralization()
    {
        return isGeneralization;
    }

    public int getEvaluationCount()
    {
        return evaluationCount;
    }

    @NotNull
    public String getInstruction()
    {
        return instruction;
    }

    public int getInstructionOffset()
    {
        return instructionOffset;
    }

    public List<BranchTargetRecord> getUpdatedEvaluationStack()
    {
        return updatedEvaluationStack;
    }

    @NotNull
    public List<String> getVariablesBefore()
    {
        return variablesBefore;
    }

    @NotNull
    public List<String> getStackBefore()
    {
        return stackBefore;
    }

    public List<InstructionBlockEvaluationRecord> getJsrBlockEvaluations()
    {
        return jsrBlockEvaluations;
    }
}
