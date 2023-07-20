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
import proguard.evaluation.BasicBranchUnit;
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
    private final Gson gson;
    private final StateTracker stateTracker = new StateTracker();
    private final Deque<List<InstructionBlockEvaluationRecord>> subRoutineTrackers;

    public MachinePrinter()
    {
        subRoutineTrackers = new ArrayDeque<>();
        gson=new GsonBuilder().setPrettyPrinting().create();
    }

    public List<InstructionBlockEvaluationRecord> getLastBlockEvaluationTracker() {
        if (subRoutineTrackers.isEmpty()) {
            return null;
        }
        return subRoutineTrackers.peekLast();
    }

    public InstructionBlockEvaluationRecord getLastBlockEvaluation() {
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

    // Code attribute level:
    @Override
    public void startCodeAttribute(Clazz clazz, Method method, proguard.classfile.attribute.CodeAttribute codeAttribute, Variables parameters)
    {
        // Register the current code attribute
        stateTracker.codeAttributes.add(new CodeAttributeRecord(
                clazz.getName(), method.getName(clazz) + method.getDescriptor(clazz), formatValueList(parameters)
        ));

        // Clear the subroutine recursion tracker
        subRoutineTrackers.clear();
        subRoutineTrackers.add(stateTracker.getLastCodeAttribute().blockEvaluations);

        // Read out all instructions in this codeAttribute
        byte[] code = codeAttribute.code;
        int offset = 0;
        while (offset < code.length) {
            Instruction instruction = InstructionFactory.create(code, offset);
            stateTracker.getLastCodeAttribute().instructions.add(
                    new InstructionRecord(
                            offset, instruction.toString()
                    ));

            offset += instruction.length(offset);
        }
    }


    @Override
    public void registerException(Clazz clazz, Method method, proguard.classfile.attribute.CodeAttribute codeAttribute, PartialEvaluator evaluator, Throwable cause)
    {
        // Register an exception in the top level stateTracker
        stateTracker.getLastCodeAttribute().error = new ErrorRecord(
                getLastBlockEvaluation().getLastEvaluation().instructionOffset, cause.getMessage()
        );
    }


    // Exceptions
    @Override
    public void startExceptionHandlingForBlock(Clazz clazz, Method method, int startOffset, int endOffset)
    {

    }

    @Override
    public void registerExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        // Register an exception handler being evaluated
        stateTracker.getLastCodeAttribute().blockEvaluations.add(new InstructionBlockEvaluationRecord(
                null, null, info.u2handlerPC
        ));

        // No need to copy branch stack
        ClassConstant constant =(ClassConstant) ((ProgramClass) clazz).getConstant(info.u2catchType);
       getLastBlockEvaluation().exceptionHandlerInfo =
                new ExceptionHandlerRecord(
                        startPC, endPC, info.u2handlerPC, constant == null ? "java/lang/Throwable" : constant.getName(clazz)
                );
    }

    @Override
    public void registerUnusedExceptionHandler(Clazz clazz, Method method, int startPC, int endPC, ExceptionInfo info)
    {
        System.out.println("temp");
    }


    // Results
    @Override
    public void evaluationResults(Clazz clazz, Method method, proguard.classfile.attribute.CodeAttribute codeAttribute, PartialEvaluator evaluator)
    {

    }



    // Instruction block level
    @Override
    public void startInstructionBlock(Clazz clazz, Method method, CodeAttribute codeAttribute,
                                      TracedVariables startVariables, TracedStack startStack, int startOffset)
    {
        // Start of a single block evaluation

        // If the last evaluation was handling an exception, this one is also
        InstructionBlockEvaluationRecord blockTracker = null;
        ExceptionHandlerRecord exInfo = null;
        if (getLastBlockEvaluation() != null)
        {
            blockTracker = getLastBlockEvaluation();
            exInfo = blockTracker.exceptionHandlerInfo;
        }

        // If the last blockTracker is not initialized, it is one created by registerException; initialize it
        if (blockTracker != null && exInfo != null && blockTracker.evaluations.isEmpty()) {
            blockTracker.startVariables = formatValueList(startVariables);
            blockTracker.startStack = formatValueList(startStack);
        }
        else
        {
            // Copy the last instruction block
            InstructionBlockEvaluationRecord lastBlock =
                    getLastBlockEvaluation();
            List<BranchTargetRecord> branchStack = new ArrayList<>();
            if (lastBlock != null)
            {
                if (lastBlock.getLastEvaluation() != null && lastBlock.getLastEvaluation().updatedEvaluationStack != null)
                {
                    branchStack = new ArrayList<>(lastBlock.getLastEvaluation().updatedEvaluationStack);
                }
                else
                {
                    branchStack = new ArrayList<>(lastBlock.branchEvaluationStack);
                }
            }
            if (!branchStack.isEmpty())
            {
                branchStack.remove(branchStack.size()-1);
            }

            getLastBlockEvaluationTracker().add(
                    new InstructionBlockEvaluationRecord(
                            formatValueList(startVariables), formatValueList(startStack), startOffset
                    ));
            getLastBlockEvaluation().exceptionHandlerInfo = exInfo;
            getLastBlockEvaluation().branchEvaluationStack= branchStack;
        }
    }

    @Override
    public void startBranchCodeBlockEvaluation(List<PartialEvaluator.InstructionBlock> branchStack)
    {

    }

    @Override
    public void instructionBlockDone(Clazz clazz,
                                     Method method,
                                     proguard.classfile.attribute.CodeAttribute codeAttribute,
                                     TracedVariables startVariables,
                                     TracedStack startStack,
                                     int startOffset)
    {

    }


    // Instruction level:
    @Override
    public void skipInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                     TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        getLastBlockEvaluation().evaluations.add(
                InstructionEvaluationRecord.seenIndicator());
    }

    @Override
    public void generalizeInstructionBlock(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        getLastBlockEvaluation().evaluations.add(
                InstructionEvaluationRecord.generalizationIndicator(evaluationCount));
    }

    @Override
    public void startInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction,
                                           TracedVariables variablesBefore, TracedStack stackBefore, int evaluationCount)
    {
        InstructionEvaluationRecord prevEval =
               getLastBlockEvaluation().getLastEvaluation();
        if (prevEval != null && prevEval.isGeneralization != null && prevEval.isGeneralization) {
            prevEval.instruction = instruction.toString();
            prevEval.instructionOffset = instructionOffset;
            prevEval.variablesBefore = formatValueList(variablesBefore);
            prevEval.stackBefore = formatValueList(stackBefore);
        } else
        {
            getLastBlockEvaluation().evaluations.add(
                    InstructionEvaluationRecord.instructionTracker(
                            instruction.toString(), instructionOffset, null,
                            formatValueList(variablesBefore), formatValueList(stackBefore)
                    )
            );
        }
    }

    @Override
    public void afterInstructionEvaluation(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesAfter, TracedStack stackAfter, BasicBranchUnit branchUnit, InstructionOffsetValue branchTarget)
    {

    }

    @Override
    public void definitiveBranch(Clazz clazz, Method method, int instructionOffset, Instruction instruction, TracedVariables variablesAfter, TracedStack stackAfter, InstructionOffsetValue branchTargets)
    {

    }

    @Override
    public void registerAlternativeBranch(Clazz clazz, Method method, int fromInstructionOffset, Instruction fromInstruction, TracedVariables variablesAfter, TracedStack stackAfter, int branchIndex, int branchTargetCount, int offset)
    {
        InstructionBlockEvaluationRecord blockEval = getLastBlockEvaluation();
        InstructionEvaluationRecord lastEval = blockEval.getLastEvaluation();

        if (lastEval.updatedEvaluationStack == null) {
            lastEval.updatedEvaluationStack = new ArrayList<>(blockEval.branchEvaluationStack);
        }
        lastEval.updatedEvaluationStack.add(new BranchTargetRecord(
            formatValueList(variablesAfter), formatValueList(stackAfter), offset
        ));
    }


    // Subroutine:
    @Override
    public void startSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd)
    {
        getLastBlockEvaluation().getLastEvaluation().jsrBlockEvaluations= new ArrayList<>();
        subRoutineTrackers.offer(getLastBlockEvaluation().getLastEvaluation().jsrBlockEvaluations);
    }

    @Override
    public void generalizeSubroutine(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int subroutineStart, int subroutineEnd)
    {
    }

    @Override
    public void endSubroutine(Clazz clazz, Method method, TracedVariables variablesAfter, TracedStack stackAfter, int subroutineStart, int subroutineEnd)
    {
        subRoutineTrackers.pop();
    }


    // Access functions
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
