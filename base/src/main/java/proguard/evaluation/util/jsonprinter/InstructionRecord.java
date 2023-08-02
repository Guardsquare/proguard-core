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
 * DTO to track a single instruction.
 */
class InstructionRecord implements JsonSerializable
{
    /**
     * The offset of the instruction.
     */
    private final int offset;

    /**
     * String representation of the instruction.
     */
    @NotNull
    private final String instruction;

    /**
     * Contains the final result computations from the partial evaluator regarding the variables of this instruction.
     */
    private List<String> finalVariablesBefore;

    /**
     * Contains the final result computations from the partial evaluator regarding the stack of this instruction.
     */
    private List<String> finalStackBefore;

    /**
     * Contains the final result computations from the partial evaluator regarding the target instructions of this instruction.
     */
    private List<Integer> finalTargetInstructions;

    /**
     * Contains the final result computations from the partial evaluator regarding the source instructions of this instruction.
     */
    private List<Integer> finalOriginInstructions;

    public InstructionRecord(int offset, @NotNull String instruction)
    {
        this.offset = offset;
        this.instruction = instruction;
    }

    @Override
    public StringBuilder toJson(StringBuilder builder)
    {
        builder.append("{");
        JsonPrinter.toJson("offset", offset, builder).append(",");
        JsonPrinter.toJson("instruction", instruction, builder);
        if (finalVariablesBefore != null)
        {
            builder.append(",");
            JsonPrinter.stringListToJson("finalVariablesBefore", finalVariablesBefore, builder);
        }
        if (finalStackBefore != null)
        {
            builder.append(",");
            JsonPrinter.stringListToJson("finalStackBefore", finalStackBefore, builder);
        }
        if (finalTargetInstructions != null)
        {
            builder.append(",");
            JsonPrinter.intListToJson("finalTargetInstructions", finalTargetInstructions, builder);
        }
        if (finalOriginInstructions != null)
        {
            builder.append(",");
            JsonPrinter.intListToJson("finalOriginInstructions", finalOriginInstructions, builder);
        }
        return builder.append("}");
    }

    public List<String> getFinalStackBefore()
    {
        return finalStackBefore;
    }

    public void setFinalStackBefore(List<String> finalStackBefore)
    {
        this.finalStackBefore = finalStackBefore;
    }

    public List<String> getFinalVariablesBefore()
    {
        return finalVariablesBefore;
    }

    public void setFinalVariablesBefore(List<String> finalVariablesBefore)
    {
        this.finalVariablesBefore = finalVariablesBefore;
    }

    public List<Integer> getFinalTargetInstructions()
    {
        return finalTargetInstructions;
    }

    public void setFinalTargetInstructions(List<Integer> finalTargetInstructions)
    {
        this.finalTargetInstructions = finalTargetInstructions;
    }

    public List<Integer> getFinalOriginInstructions()
    {
        return finalOriginInstructions;
    }

    public void setFinalOriginInstructions(List<Integer> finalOriginInstructions)
    {
        this.finalOriginInstructions = finalOriginInstructions;
    }

    public int getOffset()
    {
        return offset;
    }

    @NotNull
    public String getInstruction()
    {
        return instruction;
    }
}
