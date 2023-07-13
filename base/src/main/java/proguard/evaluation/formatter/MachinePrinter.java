package proguard.evaluation.formatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.instruction.Instruction;
import proguard.classfile.instruction.visitor.InstructionVisitor;
import proguard.evaluation.PartialEvaluator;


/**
 * Capable of printing machine readable output (JSON) xp
 *
 * {
 *     "instructions": [
 *          {
 *              "representation": string,
 *              "offset": int,
 *              "stack"?: string,
 *              "variables"?: string,
 *              evaluated: bool,
 *          }...
 *     ],
 *     "clazz": string,
 *     "method": string,
 *     "error"?: {
 *         offset: int,
 *         message: string,
 *         stacktrace: string,
 *     }
 * }
 */
public class MachinePrinter implements InstructionVisitor
{
    private final static Logger logger = LogManager.getLogger(MachinePrinter.class);

    @Override
    public void visitAnyInstruction(Clazz clazz, Method method, CodeAttribute codeAttribute, int offset, Instruction instruction)
    {
        logger.warn(instruction);
    }
}
