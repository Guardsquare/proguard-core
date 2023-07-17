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
    class StateTracker {

    }

    static class InstructionDTO
    {
        private final String instruction;
        private final int offset;
        private final String Stack;
        private final String variables;

        public InstructionDTO(String instruction, int offset, String stack, String variables)
        {
            this.instruction = instruction;
            this.offset = offset;
            Stack = stack;
            this.variables = variables;
        }
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private PartialEvaluator evaluator;

    private final Map<String, Map<String, List<InstructionDTO>>> mappy;

    public MachinePrinter() {
        mappy = new HashMap<>();
    }

    public void setEvaluator(PartialEvaluator evaluator) {
        this.evaluator = evaluator;
    }

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
