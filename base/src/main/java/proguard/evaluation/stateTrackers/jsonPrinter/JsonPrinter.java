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
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.ExceptionInfo;
import proguard.classfile.constant.ClassConstant;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.InstructionFactory;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.Stack;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;
import proguard.evaluation.Variables;
import proguard.evaluation.stateTrackers.PartialEvaluatorStateTracker;
import proguard.evaluation.value.InstructionOffsetValue;
import proguard.evaluation.value.Value;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * Tracks the state of the partial evaluator able to provide debug information in JSON format
 */
public class JsonPrinter implements PartialEvaluatorStateTracker
{
    /**
     * Debug flag for this class, when enabled, JSON is pretty printed.
     */
    private static final boolean DEBUG = true;

    /**
     * Tracks the state of the partial evaluator
     */
    private final StateTracker stateTracker;

    /**
     * Traces the current depth of JSR recursion.
     * All accesses to an InstructionBlockEvaluationRecord should be done through here
     */
    private final Deque<List<InstructionBlockEvaluationRecord>> subRoutineTrackers;

    private final Clazz clazzFilter;

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
        this.subRoutineTrackers = new ArrayDeque<>();
        this.clazzFilter = clazzFilter;
        this.methodFilter = methodFilter;
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
            // Do nothing
        }
    }

    /**
     * @return the last relevant list of InstructionBlockEvaluationRecord referenced by PE
     */
    private List<InstructionBlockEvaluationRecord> getSubroutineInstructionBlockEvaluationTracker() {
        if (subRoutineTrackers.isEmpty()) {
            return null;
        }
        return subRoutineTrackers.peekLast();
    }

    /**
     * @return the last relevant InstructionBlockEvaluationRecord referenced by the PE
     */
    private InstructionBlockEvaluationRecord getLastInstructionBlockEvaluation() {
        if (subRoutineTrackers.isEmpty()) {
            return null;
        }
        List<InstructionBlockEvaluationRecord> list = subRoutineTrackers.peekLast();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    private List<String> formatValueList(Variables variables) {
        List<String> res = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            Value val = variables.getValue(i);
            res.add(val == null ? "empty" : val.toString());
        }
        return res;
    }

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

    /************************
     * Code attribute level *
     ************************/
    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
        if (shouldSkip(clazz, method)) return;

        // Read out all instructions in this codeAttribute
        List<InstructionRecord> instructions = new ArrayList<>();
        byte[] code = codeAttribute.code;
        int offset = 0;
        while (offset < code.length) {
            Instruction instruction = InstructionFactory.create(code, offset);
            instructions.add(new InstructionRecord(offset, instruction.toString()));
            offset += instruction.length(offset);
        }

        // Create the new CodeAttributeRecord
        CodeAttributeRecord attributeRecord = new CodeAttributeRecord(clazz.getName(),
                method.getName(clazz) + method.getDescriptor(clazz), formatValueList(parameters), instructions);

        // Clear the subroutine recursion tracker and add the current
        subRoutineTrackers.clear();
        subRoutineTrackers.add(attributeRecord.getBlockEvaluations());

        // Register the current code attribute
        stateTracker.getCodeAttributes().add(attributeRecord);
    }

    @Override
    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                  PartialEvaluator evaluator, Throwable cause)
    {
        if (shouldSkip(clazz, method)) return;

        stateTracker.getLastCodeAttribute().setError(new ErrorRecord(
                getLastInstructionBlockEvaluation().getLastInstructionEvaluation().getInstructionOffset(),
                cause.getMessage()));
    }


    /**************
     * Exceptions *
     **************/
    @Override
    public void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        if (shouldSkip(clazz, method)) return;

        // TODO: currently instantiated with null and later corrected, can we do better?

        ClassConstant constant = (ClassConstant) ((ProgramClass) clazz).getConstant(info.u2catchType);
        ExceptionHandlerRecord exceptionHandlerInfo = new ExceptionHandlerRecord(startPC, endPC,
                info.u2handlerPC, constant == null ? "java/lang/Throwable" : constant.getName(clazz));

        // Register an exception handler being evaluated. NOTE: do not copy the branch stack (should be empty but still)
        getSubroutineInstructionBlockEvaluationTracker().add(
                new InstructionBlockEvaluationRecord(null, null,
                        info.u2handlerPC, exceptionHandlerInfo, new ArrayList<>()));
    }


    /***********
     * Results *
     ***********/
    @Override
    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {
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



    /***************************
     * Instruction block level *
     ***************************/
    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables startVariables, TracedStack startStack, int startOffset)
    {
        if (shouldSkip(clazz, method)) return;

        // If the last evaluation was handling an exception, this one is also, copy it over
        InstructionBlockEvaluationRecord lastBlock = getLastInstructionBlockEvaluation();

        // If the last blockTracker is not initialized, it is one created by registerException, initialize it
        if (lastBlock != null && lastBlock.getExceptionHandlerInfo() != null && lastBlock.getEvaluations().isEmpty()) {
            lastBlock.setStartVariables(formatValueList(startVariables));
            lastBlock.setStartStack(formatValueList(startStack));
        }
        else
        {
            ExceptionHandlerRecord exceptionHandlerInfo = null;

            List<BranchTargetRecord> branchStack = new ArrayList<>();
            // If there is a last block, copy the branch stack, either from last instruction or last block
            if (lastBlock != null)
            {
                InstructionEvaluationRecord lastInstruction = lastBlock.getLastInstructionEvaluation();
                if (lastInstruction != null && lastInstruction.getUpdatedEvaluationStack() != null)
                {
                    branchStack = new ArrayList<>(lastBlock.getLastInstructionEvaluation().getUpdatedEvaluationStack());
                }
                else
                {
                    branchStack = new ArrayList<>(getLastInstructionBlockEvaluation().getBranchEvaluationStack());
                }

                // Copy the exceptionHandlerInfo from the last block
                exceptionHandlerInfo = lastBlock.getExceptionHandlerInfo();
            }

            // Whatever the branch stack, if possible, pop, it is the block you start now
            if (!branchStack.isEmpty())
            {
                BranchTargetRecord stackHead = branchStack.remove(branchStack.size()-1);
                assert stackHead.getStartOffset() == startOffset;
            }

            // Add the newly created InstructionBlockEvaluationRecord to the current subroutine block tracker
            getSubroutineInstructionBlockEvaluationTracker().add(new InstructionBlockEvaluationRecord(
                    formatValueList(startVariables), formatValueList(startStack), startOffset,
                    exceptionHandlerInfo, branchStack));
        }
    }


    /*********************
     * Instruction level *
     *********************/
    @Override
    public void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                     TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        if (shouldSkip(clazz, method)) return;

        getLastInstructionBlockEvaluation().getEvaluations().add(
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

        getLastInstructionBlockEvaluation().getEvaluations().add(
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

        InstructionEvaluationRecord prevEval = getLastInstructionBlockEvaluation().getLastInstructionEvaluation();
        if (prevEval == null || prevEval.getInstructionOffset() != instructionOffset ||
                prevEval.getEvaluationCount() != evaluationCount) {
            getLastInstructionBlockEvaluation().getEvaluations().add(
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

        InstructionBlockEvaluationRecord lastBlock = getLastInstructionBlockEvaluation();
        InstructionEvaluationRecord lastInstruction = lastBlock.getLastInstructionEvaluation();

        // If we don't already know, register that this is a branching instruction
        if (lastInstruction.getUpdatedEvaluationStack() == null) {
            lastInstruction.setUpdatedEvaluationStack(new ArrayList<>(lastBlock.getBranchEvaluationStack()));
        }
        // Add this branch
        lastInstruction.getUpdatedEvaluationStack().add(new BranchTargetRecord(
            formatValueList(variablesAfter), formatValueList(stackAfter), offset
        ));
    }


    /**************
     * Subroutine *
     **************/
    @Override
    public void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack,
                                int subroutineStart, int subroutineEnd)
    {
        if (shouldSkip(clazz, method)) return;

        InstructionEvaluationRecord lastInstruction = getLastInstructionBlockEvaluation().getLastInstructionEvaluation();
        lastInstruction.setJsrBlockEvaluations(new ArrayList<>());
        subRoutineTrackers.offer(lastInstruction.getJsrBlockEvaluations());
    }

    @Override
    public void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter,
                              int subroutineStart, int subroutineEnd)
    {
        if (shouldSkip(clazz, method)) return;

        subRoutineTrackers.pop();
    }
}
