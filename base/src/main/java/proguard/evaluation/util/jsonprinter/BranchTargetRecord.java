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

class BranchTargetRecord implements JsonSerializable
{
    /**
     * Variables at the start of the block evaluation.
     */
    @NotNull
    private final List<String> startVariables;

    /**
     * Stack at the start of the block evaluation.
     */
    @NotNull
    private final List<String> startStack;

    /**
     * Instruction offset of the first instruction of the block.
     */
    private final int startOffset;

    public BranchTargetRecord(@NotNull List<String> variables, @NotNull List<String> stack, int startOffset)
    {
        this.startVariables = variables;
        this.startStack = stack;
        this.startOffset = startOffset;
    }

    @Override
    public StringBuilder toJson(StringBuilder builder)
    {
        builder.append("{");
        JsonPrinter.toJson("startOffset", startOffset, builder).append(",");
        JsonPrinter.stringListToJson("startStack", startStack, builder).append(",");
        return JsonPrinter.stringListToJson("startVariables", startVariables, builder).append("}");
    }

    @NotNull
    public List<String> getStartVariables()
    {
        return startVariables;
    }

    @NotNull
    public List<String> getStartStack()
    {
        return startStack;
    }

    public int getStartOffset()
    {
        return startOffset;
    }
}
