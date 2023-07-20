package proguard.evaluation.stateTrackers.machinePrinter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import proguard.evaluation.value.Value;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


/**
 * Capable of printing machine-readable output (JSON) xp
 * {
 *      "codeAttribute": {
 *          clazz
 *          method
 *          instructions: {offset, instruction}[],
 *          parameters: String(Value)[],
 *          error?: {
 *              clazz,
 *              method,
 *              message,
 *              stacktrace,
 *          }
 *          blockEvaluations: {
 *              startOffset,
 *              startVariables,
 *              startStack,
 *              blockEvaluationStack
 *              exceptionInfo?: { startOffset, endOffset, catchType (just string name) }
 *              evaluations: {
 *                  isSeenBefore,
 *                  isGeneralization,
 *                  timesSeen,
 *                  offset,
 *                  instruction
 *                  updatedEvaluationStack,
 *                  variablesBefore
 *                  stackBefore
 *              }[]
 *          }[]
 *      }[]
 * }
 */

public class MachinePrinter implements PartialEvaluatorStateTracker
{
    /**
     * GSON object used to create json string format
     */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Tracks the state of the partial evaluator
     */
    private final StateTracker stateTracker = new StateTracker();

    /**
     * Traces the current depth of JSR recursion.
     * All accesses to an InstructionBlockEvaluationRecord should be done through here
     */
    private final Deque<List<InstructionBlockEvaluationRecord>> subRoutineTrackers = new ArrayDeque<>();

    /**
     * @return the last relevant list of InstructionBlockEvaluationRecord referenced by PE
     */
    public List<InstructionBlockEvaluationRecord> getSubroutineInstructionBlockEvaluationTracker() {
        if (subRoutineTrackers.isEmpty()) {
            return null;
        }
        return subRoutineTrackers.peekLast();
    }

    /**
     * @return the last relevant InstructionBlockEvaluationRecord referenced by the PE
     */
    public InstructionBlockEvaluationRecord getLastInstructionBlockEvaluation() {
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

    /************************
     * Code attribute level *
     ************************/
    @Override
    public void startCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, Variables parameters)
    {
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
        subRoutineTrackers.add(attributeRecord.blockEvaluations);

        // Register the current code attribute
        stateTracker.codeAttributes.add(attributeRecord);
    }

    @Override
    public void registerException(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                  PartialEvaluator evaluator, Throwable cause)
    {
        stateTracker.getLastCodeAttribute().error = new ErrorRecord(
                getLastInstructionBlockEvaluation().getLastInstructionEvaluation().instructionOffset, cause.getMessage());
    }


    /**************
     * Exceptions *
     **************/
    @Override
    public void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        // TODO: currently instantiated with null and later corrected, can we do better?

        ClassConstant constant = (ClassConstant) ((ProgramClass) clazz).getConstant(info.u2catchType);
        ExceptionHandlerRecord exceptionHandlerInfo = new ExceptionHandlerRecord(startPC, endPC,
                info.u2handlerPC, constant == null ? "java/lang/Throwable" : constant.getName(clazz));

        // Register an exception handler being evaluated. NOTE: do not copy the branch stack (should be empty but still)
        getSubroutineInstructionBlockEvaluationTracker().add(
                new InstructionBlockEvaluationRecord(null, null, info.u2handlerPC, exceptionHandlerInfo, null));
    }


    /***********
     * Results *
     ***********/
    @Override
    public void evaluationResults(Clazz clazz, Method method, CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {
        // TODO: we no want?
    }



    /***************************
     * Instruction block level *
     ***************************/
    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables startVariables, TracedStack startStack, int startOffset)
    {
        // If the last evaluation was handling an exception, this one is also, copy it over
        InstructionBlockEvaluationRecord lastBlock = getLastInstructionBlockEvaluation();

        // If the last blockTracker is not initialized, it is one created by registerException, initialize it
        if (lastBlock != null && lastBlock.exceptionHandlerInfo != null && lastBlock.evaluations.isEmpty()) {
            lastBlock.startVariables = formatValueList(startVariables);
            lastBlock.startStack = formatValueList(startStack);
            // No need to copy branching information
        }
        else
        {
            ExceptionHandlerRecord exceptionHandlerInfo = null;

            List<BranchTargetRecord> branchStack = new ArrayList<>();
            // If there is a last block, copy the branch stack, either from last instruction or last block
            if (lastBlock != null)
            {
                InstructionEvaluationRecord lastInstruction = lastBlock.getLastInstructionEvaluation();
                if (lastInstruction != null && lastInstruction.updatedEvaluationStack != null)
                {
                    branchStack = new ArrayList<>(lastBlock.getLastInstructionEvaluation().updatedEvaluationStack);
                }
                else
                {
                    branchStack = new ArrayList<>(getLastInstructionBlockEvaluation().branchEvaluationStack);
                }

                // Copy the exceptionHandlerInfo from the last block
                exceptionHandlerInfo = lastBlock.exceptionHandlerInfo;
            }

            // Whatever the branch stack, if possible, pop, it is the block you start now
            if (!branchStack.isEmpty())
            {
                BranchTargetRecord stackHead = branchStack.remove(branchStack.size()-1);
                assert stackHead.startOffset == startOffset;
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
        getLastInstructionBlockEvaluation().evaluations.add(
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
        getLastInstructionBlockEvaluation().evaluations.add(
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
        InstructionEvaluationRecord prevEval = getLastInstructionBlockEvaluation().getLastInstructionEvaluation();
        if (prevEval == null || prevEval.instructionOffset != instructionOffset || prevEval.timesSeen != evaluationCount) {
            getLastInstructionBlockEvaluation().evaluations.add(
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
        InstructionBlockEvaluationRecord lastBlock = getLastInstructionBlockEvaluation();
        InstructionEvaluationRecord lastInstruction = lastBlock.getLastInstructionEvaluation();

        // If we don't already know, register that this is a branching instruction
        if (lastInstruction.updatedEvaluationStack == null) {
            lastInstruction.updatedEvaluationStack = new ArrayList<>(lastBlock.branchEvaluationStack);
        }
        // Add this branch
        lastInstruction.updatedEvaluationStack.add(new BranchTargetRecord(
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
        InstructionEvaluationRecord lastInstruction = getLastInstructionBlockEvaluation().getLastInstructionEvaluation();
        lastInstruction.jsrBlockEvaluations = new ArrayList<>();
        subRoutineTrackers.offer(lastInstruction.jsrBlockEvaluations);
    }

    @Override
    public void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter,
                              int subroutineStart, int subroutineEnd)
    {
        subRoutineTrackers.pop();
    }

    public String getJson() {
        return gson.toJson(stateTracker);
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
}
