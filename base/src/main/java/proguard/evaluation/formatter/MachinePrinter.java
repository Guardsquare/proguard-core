package proguard.evaluation.formatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.evaluation.PartialEvaluator;
import proguard.evaluation.TracedStack;
import proguard.evaluation.TracedVariables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Capable of printing machine-readable output (JSON) xp
 *
 * {
 *     "evaluation-steps": [
 *          { BlockEvaluation(?) - do recursive evaluations happen often? Or only when using jsr?
 *              "class",
 *              "method",
 *              "startVariables",
 *              "startStack",
 *              "startOffset"
 *              "evaluations": [
 *                  {
 *                      "isSeenBefore": bool,
 *                      "isGeneralization": bool,  --- Check if this breaks????
 *                      "instruction": str,
 *                      "instructionOffset"
 *                      "updatedEvaluationStack"?: new stack
 *                      "variablesBefore"
 *                      "stackBefore"
 *                  }
 *              ]
 *          }
 *     ]
 *     "error"?: {
 *         clazz
 *         method
 *         offset: int,
 *         message: string,
 *         stacktrace: string,
 *     }
 * }
 */
public class MachinePrinter implements InstructionVisitor
{
    static class StateTracker {
        static class MethodTracker {
            class InstructionBlock {
                public TracedVariables variables;
                public TracedStack stack;
                public int startOffset;

                public InstructionBlock(TracedVariables variables, TracedStack stack, int startOffset)
                {
                    this.variables=variables;
                    this.stack=stack;
                    this.startOffset=startOffset;
                }
            }
            static class InstructionTracker {
                public boolean isSeenBefore;
                public boolean isGeneralization;
                public Instruction instruction;
                public int instructionOffset;
                public List<InstructionBlock> evaluationBlockStack;
                public TracedVariables variablesBefore;
                public TracedStack stackBefore;

                public InstructionTracker(boolean isSeenBefore, boolean isGeneralization, Instruction instruction, int instructionOffset, List<InstructionBlock> evaluationBlockStack, TracedVariables variablesBefore, TracedStack stackBefore)
                {
                    this.isSeenBefore=isSeenBefore;
                    this.isGeneralization=isGeneralization;
                    this.instruction=instruction;
                    this.instructionOffset=instructionOffset;
                    this.evaluationBlockStack=evaluationBlockStack;
                    this.variablesBefore=variablesBefore;
                    this.stackBefore=stackBefore;
                }
            }
            public Clazz clazz;
            public Method method;
            public TracedVariables startVariables;
            public TracedStack startStack;
            public int startOffset;

            public MethodTracker(Clazz clazz, Method method, TracedVariables startVariables, TracedStack startStack, int startOffset)
            {
                this.clazz=clazz;
                this.method=method;
                this.startVariables=startVariables;
                this.startStack=startStack;
                this.startOffset=startOffset;
            }
        }
        static class ErrorTracker
        {
            public Clazz clazz;
            public MethodTracker method;
            public int instructionOffset;
            public String message;
            Throwable cause;

            public ErrorTracker(Clazz clazz, MethodTracker method, int instructionOffset, String message, Throwable cause)
            {
                this.clazz=clazz;
                this.method=method;
                this.instructionOffset=instructionOffset;
                this.message=message;
                this.cause=cause;
            }
        }
        public final List<MethodTracker> methods = new ArrayList<>();
        public ErrorTracker error;
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private StateTracker stateTracker = new StateTracker();

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        //Map<String, List<InstructionDTO>> methods = mappy.computeIfAbsent(clazz.getName(), k -> new HashMap<>());
//
        //List<InstructionDTO> instructions =
        //        methods.computeIfAbsent(method.getName(clazz), k -> new ArrayList<>());
//
        //InstructionDTO instructionDTO = new InstructionDTO(
        //        instruction.toString(),
        //        offset,
        //        evaluator.getStackBefore(offset).toString(),
        //        evaluator.getVariablesBefore(offset).toString()
        //);
        //instructions.add(instructionDTO);

        // System.out.println(gson.toJson(mappy));
    }
}
