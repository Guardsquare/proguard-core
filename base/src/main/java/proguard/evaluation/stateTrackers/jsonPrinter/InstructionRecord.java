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

package proguard.evaluation.stateTrackers.jsonPrinter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * DTO to track a single instruction
 */
class InstructionRecord
{
    /**
     * The offset of the instruction
     */
    private final int offset;

    /**
     * String representation of the instruction
     */
    @NotNull
    private final String instruction;

    private List<String> finalStackBefore;

    private List<String> finalVariablesBefore;

    private List<Integer> finalTargetInstructions;

    private List<Integer> finalSourceInstructions;

    public InstructionRecord(int offset, @NotNull String instruction)
    {
        this.offset = offset;
        this.instruction = instruction;
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

    public List<Integer> getFinalSourceInstructions()
    {
        return finalSourceInstructions;
    }

    public void setFinalSourceInstructions(List<Integer> finalSourceInstructions)
    {
        this.finalSourceInstructions = finalSourceInstructions;
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
