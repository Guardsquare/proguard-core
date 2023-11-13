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
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeNameFilter;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.Stack;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.evaluation.Variables;
import proguard.evaluation.util.PartialEvaluatorStateTracker;
import proguard.evaluation.value.InstructionOffsetValue;
import proguard.evaluation.value.Value;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Tracks the state of the partial evaluator able to provide debug information in JSON format.
 */
public class JsonPrinter implements PartialEvaluatorStateTracker
{
    /**
     * Tracks the state of the partial evaluator.
     */
    private final StateTracker stateTracker;

    /**
     * Traces the current depth of JSR recursion.
     * All accesses to an InstructionBlockEvaluationRecord should be done through here.
     */
    private final List<List<InstructionBlockEvaluationRecord>> subRoutineStack;

    /**
     * Filter to a certain clazz, only evaluations of this Clazz are processed.
     */
    private final Clazz clazzFilter;

    /**
     * Filter to a certain Method, only evaluations of this Method are processed.
     */
    private final Method methodFilter;

    public JsonPrinter()
    {
        this(null, null);
    }

    public JsonPrinter(Clazz clazzFilter)
    {
        this(clazzFilter, null);
    }

    public JsonPrinter(Clazz clazzFilter, Method methodFilter)
    {
        this.stateTracker = new StateTracker();
        this.subRoutineStack = new ArrayList<>();
        this.clazzFilter = clazzFilter;
        this.methodFilter = methodFilter;
    }

    /**
     * API to easily create a JSON debug file.
     */
    static void writeJsonDebug(Clazz clazz, Method method, String fileName) {
        writeJsonDebug(clazz, method, fileName, PartialEvaluator.Builder.create());
    }

    /**
     * API to easily create a JSON debug file. Allows to give a partial build Partial Evaluator.
     */
    static void writeJsonDebug(Clazz clazz, Method method, String fileName, PartialEvaluator.Builder builder) {
        JsonPrinter printer = new JsonPrinter();
        PartialEvaluator pe = builder.setStateTracker(printer).build();
        method.accept(clazz, new AllAttributeVisitor(new AttributeNameFilter(Attribute.CODE, pe)));
        printer.writeState(fileName);
    }

    public String getJson() {
        return stateTracker.toJson();
    }

    public void printState()
    {
        System.out.println(getJson());
    }

    public void writeState(String fileName) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(getJson());

            writer.close();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return the last relevant list of InstructionBlockEvaluationRecord referenced by PE.
     */
    private List<InstructionBlockEvaluationRecord> curBlockEvalList() {
        return subRoutineStack.get(subRoutineStack.size() - 1);
    }

    /**
     * @return the last relevant InstructionBlockEvaluationRecord referenced by the PE.
     */
    private InstructionBlockEvaluationRecord lastBlockEval() {
        List<InstructionBlockEvaluationRecord> curList = curBlockEvalList();
        if (curBlockEvalList().isEmpty()) {
            return null;
        }
        return curList.get(curList.size() - 1);
    }

    /**
     * Serialize variables.
     */
    private List<String> formatValueList(Variables variables) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            Value val = variables.getValue(i);
            res.add(val == null ? "empty" : val.toString());
        }
        return res;
    }

    /**
     * Serialize the stack.
     */
    private List<String> formatValueList(Stack stack) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < stack.size(); i++) {
            Value val = stack.getBottom(i);
            res.add(val == null ? "empty" : val.toString());
        }
        return res;
    }

    /**
     * @return whether the current class and method should be skipped/ are not tracked.
     */
    private boolean shouldSkip(Clazz clazz, Method method) {
        return (clazzFilter != null && clazzFilter != clazz) || (methodFilter != null && methodFilter != method);
    }

    // region json serialization

    static StringBuilder toJson(@NotNull String key, Function<StringBuilder, StringBuilder> callback, StringBuilder builder) {
        builder
                .append("\"")
                .append(key)
                .append("\":");
        return callback.apply(builder);
    }

    static StringBuilder toJson(@NotNull String key, @NotNull String value, StringBuilder builder)
    {
        return toJson(key, (build) -> build.append("\"").append(value).append("\""), builder);
    }

    static StringBuilder toJson(@NotNull String key, int value, StringBuilder builder)
    {
        return toJson(key, (build) -> build.append(value), builder);
    }

    static StringBuilder toJson(@NotNull String key, boolean value, StringBuilder builder)
    {
        return toJson(key, (build) -> build.append(value), builder);
    }

    static <T extends JsonSerializable> void serializeJsonSerializable(@NotNull String key, T value, StringBuilder builder)
    {
        toJson(key, value::toJson, builder);
    }

    @NotNull
    static <T extends JsonSerializable> StringBuilder listToJson(@NotNull String key, @NotNull List<T> formattableList, @NotNull StringBuilder builder) {
        return toJson(key, build -> {
            build.append("[");
            if (!formattableList.isEmpty())
            {
                formattableList.get(0).toJson(build);
            }
            for (int index = 1; index < formattableList.size(); index++)
            {
                build.append(",");
                formattableList.get(index).toJson(build);
            }
            build.append("]");
            return build;
        }, builder);
    }

    @NotNull
    static StringBuilder stringListToJson(@NotNull String key, @NotNull List<String> formattableList, @NotNull StringBuilder builder) {
        return toJson(key, build -> {
            build.append("[");
            if (!formattableList.isEmpty())
            {
                build.append("\"").append(formattableList.get(0)).append("\"");
            }
            for (int index = 1; index < formattableList.size(); index++)
            {
                build.append(",\"");
                build.append(formattableList.get(index)).append("\"");
            }
            build.append("]");
            return build;
        }, builder);
    }

    @NotNull
    static StringBuilder intListToJson(@NotNull String key, @NotNull List<Integer> formattableList, @NotNull StringBuilder builder) {
        return toJson(key, build -> {
            build.append("[");
            if (!formattableList.isEmpty())
            {
                build.append(formattableList.get(0));
            }
            for (int index = 1; index < formattableList.size(); index++)
            {
                build.append(",");
                build.append(formattableList.get(index));
            }
            build.append("]");
            return build;
        }, builder);
    }

    // endregion

    // region Code attribute level

    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        if (shouldSkip(clazz, method)) return;

        // Read out all instructions in this codeAttribute.
        List<InstructionRecord> instructions = new ArrayList<>();
        byte[] code = codeAttribute.code;
        int offset = 0;
        while (offset < codeAttribute.u4codeLength) {
            Instruction instruction = InstructionFactory.create(code, offset);
            instructions.add(new InstructionRecord(offset, instruction.toString()));
            offset += instruction.length(offset);
        }

        // Create the new CodeAttributeRecord.
        CodeAttributeRecord attributeRecord = new CodeAttributeRecord(clazz.getName(),
                method.getName(clazz) + method.getDescriptor(clazz), formatValueList(parameters), instructions);

        // Clear the subroutine recursion tracker and add the current.
        subRoutineStack.clear();
        subRoutineStack.add(attributeRecord.getBlockEvaluations());

        // Register the current code attribute.
        stateTracker.getCodeAttributes().add(attributeRecord);
    }

    @Override
    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                  PartialEvaluator evaluator, Throwable cause)
    {
        if (shouldSkip(clazz, method)) return;

        stateTracker.getLastCodeAttribute().setError(new ErrorRecord(
                lastBlockEval().getLastInstructionEvaluation().getInstructionOffset(),
                cause.getMessage()));
    }

    // endregion

    // region Exception handling

    @Override
    public void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        if (shouldSkip(clazz, method)) return;

        ClassConstant constant = (ClassConstant) ((ProgramClass) clazz).getConstant(info.u2catchType);
        ExceptionHandlerRecord exceptionHandlerInfo = new ExceptionHandlerRecord(startPC, endPC,
                info.u2handlerPC, constant == null ? "java/lang/Throwable" : constant.getName(clazz));

        // Register an exception handler being evaluated. NOTE: do not copy the branch stack.
        curBlockEvalList().add(
                new InstructionBlockEvaluationRecord(null, null,
                        info.u2handlerPC, exceptionHandlerInfo, new ArrayList<>()));
    }

    // endregion

    // region Results

    @Override
    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {
        // loop over all instruction and fill in their final evaluation state.
        for (InstructionRecord instruction: stateTracker.getLastCodeAttribute().getInstructions()) {
            TracedVariables variablesBefore = evaluator.getVariablesBefore(instruction.getOffset());
            instruction.setFinalVariablesBefore(variablesBefore == null ? null : formatValueList(variablesBefore));

            TracedStack stackBefore = evaluator.getStackBefore(instruction.getOffset());
            instruction.setFinalStackBefore(stackBefore == null ? null : formatValueList(stackBefore));

            InstructionOffsetValue targets = evaluator.branchTargets(instruction.getOffset());
            if (targets != null) {
                instruction.setFinalTargetInstructions(new ArrayList<>());
                for (int index = 0; index < targets.instructionOffsetCount(); index++) {
                    instruction.getFinalTargetInstructions().add(targets.instructionOffset(index));
                }
            }

            InstructionOffsetValue origins = evaluator.branchOrigins(instruction.getOffset());
            if (origins != null) {
                instruction.setFinalOriginInstructions(new ArrayList<>());
                for (int index = 0; index < origins.instructionOffsetCount(); index++) {
                    instruction.getFinalOriginInstructions().add(origins.instructionOffset(index));
                }
            }
        }
    }

    // endregion

    // region Instruction block level

    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables startVariables, TracedStack startStack, int startOffset)
    {
        if (shouldSkip(clazz, method)) return;

        // If the last evaluation was handling an exception, this one is also, copy it over.
        InstructionBlockEvaluationRecord lastBlock = lastBlockEval();

        // If the last blockTracker is not initialized, it is one created by registerException, initialize it.
        if (lastBlock != null && lastBlock.getExceptionHandlerInfo() != null && lastBlock.getEvaluations().isEmpty()) {
            lastBlock.setStartVariables(formatValueList(startVariables));
            lastBlock.setStartStack(formatValueList(startStack));
        }
        else
        {
            // The current block is not a newly registered exception handler. Threat it like any other block.

            ExceptionHandlerRecord exceptionHandlerInfo = null;
            List<BranchTargetRecord> branchStack = new ArrayList<>();

            // If there is a last block, copy the branch stack, either from last instruction or last block.
            if (lastBlock != null)
            {
                InstructionEvaluationRecord lastInstruction = lastBlock.getLastInstructionEvaluation();
                if (lastInstruction != null && lastInstruction.getUpdatedEvaluationStack() != null)
                {
                    // Copy branch stack from last instruction since it changed the branch stack.
                    branchStack = new ArrayList<>(lastInstruction.getUpdatedEvaluationStack());
                }
                else
                {
                    // Last instruction did not change the branch stack, copy it from the last block.
                    branchStack = new ArrayList<>(lastBlock.getBranchEvaluationStack());
                }

                // Copy the exceptionHandlerInfo from the last block.
                exceptionHandlerInfo = lastBlock.getExceptionHandlerInfo();
            }

            // Whatever the branch stack, if possible, pop, it is the block you start now.
            if (!branchStack.isEmpty())
            {
                BranchTargetRecord stackHead = branchStack.remove(branchStack.size()-1);
                assert stackHead.getStartOffset() == startOffset;
            }

            // Add the newly created InstructionBlockEvaluationRecord to the current subroutine block tracker.
            curBlockEvalList().add(new InstructionBlockEvaluationRecord(
                    formatValueList(startVariables), formatValueList(startStack), startOffset,
                    exceptionHandlerInfo, branchStack));
        }
    }

    // endregion

    // region Instruction level

    @Override
    public void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                     TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (shouldSkip(clazz, method)) return;

        lastBlockEval().getEvaluations().add(
                new InstructionEvaluationRecord(true, false, evaluationCount,
                        instruction.toString(), instructionOffset,
                        formatValueList(variablesBefore), formatValueList(stackBefore)
                )
        );
    }

    @Override
    public void generalizeInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                           TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (shouldSkip(clazz, method)) return;

        lastBlockEval().getEvaluations().add(
                new InstructionEvaluationRecord(false, true, evaluationCount,
                        instruction.toString(), instructionOffset,
                        formatValueList(variablesBefore), formatValueList(stackBefore)
                )
        );
    }

    @Override
    public void startInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                           TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (shouldSkip(clazz, method)) return;

        InstructionEvaluationRecord prevEval = lastBlockEval().getLastInstructionEvaluation();

        // Do not add the instruction if this was already done by the generalization.
        if (prevEval == null || prevEval.getInstructionOffset() != instructionOffset ||
                prevEval.getEvaluationCount() != evaluationCount) {
            lastBlockEval().getEvaluations().add(
                    new InstructionEvaluationRecord(false, false, evaluationCount,
                            instruction.toString(), instructionOffset,
                            formatValueList(variablesBefore), formatValueList(stackBefore)
                    )
            );
        }
    }

    @Override
    public void registerAlternativeBranch(Clazz clazz, Method method, int fromInstructionOffset,
                                          Instruction fromInstruction, TracedVariables variablesAfter,
                                          TracedStack stackAfter, int branchIndex, int branchTargetCount, int offset)
    {
        if (shouldSkip(clazz, method)) return;

        InstructionBlockEvaluationRecord lastBlock = lastBlockEval();
        InstructionEvaluationRecord lastInstruction = lastBlock.getLastInstructionEvaluation();

        // If we don't already know, register that this is a branching instruction.
        if (lastInstruction.getUpdatedEvaluationStack() == null) {
            lastInstruction.setUpdatedEvaluationStack(new ArrayList<>(lastBlock.getBranchEvaluationStack()));
        }
        // Add this branch to the current instruction.
        lastInstruction.getUpdatedEvaluationStack().add(new BranchTargetRecord(
            formatValueList(variablesAfter), formatValueList(stackAfter), offset
        ));
    }

    // endregion

    // region subroutines

    @Override
    public void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack,
                                int subroutineStart, int subroutineEnd)
    {
        if (shouldSkip(clazz, method)) return;

        InstructionEvaluationRecord lastInstruction = lastBlockEval().getLastInstructionEvaluation();
        lastInstruction.setJsrBlockEvaluations(new ArrayList<>());
        subRoutineStack.add(lastInstruction.getJsrBlockEvaluations());
    }

    public void registerSubroutineReturn(Clazz clazz, Method method, int returnOffset, TracedVariables returnVariables, TracedStack returnStack)
    {
        if (shouldSkip(clazz, method)) return;

        // Get the last instruction of the previous blockEValList, this should be JSR; update the branch eval stack.
        List<InstructionBlockEvaluationRecord> blockList = subRoutineStack.get(subRoutineStack.size() - 2);
        InstructionBlockEvaluationRecord lastBlock = blockList.get(blockList.size() - 1);
        InstructionEvaluationRecord instruction = lastBlock.getLastInstructionEvaluation();
        assert instruction.getInstruction().startsWith("jsr");

        instruction.setUpdatedEvaluationStack(new ArrayList<>(lastBlock.getBranchEvaluationStack()));
        instruction.getUpdatedEvaluationStack().add(
                new BranchTargetRecord(formatValueList(returnVariables), formatValueList(returnStack), returnOffset));
    }


    @Override
    public void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter,
                              int subroutineStart, int subroutineEnd)
    {
        if (shouldSkip(clazz, method)) return;

        subRoutineStack.remove(subRoutineStack.size() - 1);
    }

    // endregion
}
