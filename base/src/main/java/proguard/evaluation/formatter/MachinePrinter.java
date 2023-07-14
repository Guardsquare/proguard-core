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
 * Capable of printing machine readable output (JSON) xp
 *
 * {
 *     classes: MAP[
 *          {
 *              methods: MAP[
 *                  {
 *                      "instructions": [
 *                          {
 *                              "representation"
 *                              "offset"
 *                              "stack"
 *                              "variables"
 *                          }
 *                      ]
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
    static class InstructionDTO
    {
        private final String instruction;
        private final int offset;
        private final String Stack;
        private final String variables;
        private final boolean evaluated;

        public InstructionDTO(String instruction, int offset, String stack, String variables, boolean evaluated)
        {
            this.instruction = instruction;
            this.offset = offset;
            Stack = stack;
            this.variables = variables;
            this.evaluated = evaluated;
        }
    }

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
        List<InstructionDTO> instructions = mappy.getOrDefault(clazz.getName(), new HashMap<>())
                .getOrDefault(method.toString(), new ArrayList<>());
        if (instructions.isEmpty()) {
            byte[] code = codeAttribute.code;
            // int offset = codeAttribute.
        }
        //
        // codeAttribute.code

        // mappy.getOrDefault(clazz.getName(), new HashMap<>())
        //         .getOrDefault(method.getName(clazz), new HashSet<>())
        //         .add(new InstructionDTO())
        // HashMap<String, String> map = new HashMap<>();
        // map.put("Instruction", instruction.toString());
        // System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(map));
    }
}
