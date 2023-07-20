package proguard.evaluation.stateTrackers.machinePrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Track the evaluation of a single code attribute (one call to visitCode attribute)
 */
class CodeAttributeRecord
{
    /**
     * Clazz this code attribute is a part of.
     */
    public String clazz;

    /**
     * Method this code attribute is from.
     */
    public String method;

    /**
     * List of instruction from this code attribute.
     */
    public List<InstructionRecord> instructions = new ArrayList<>();

    /**
     * List of parameters given to the code attribute.
     */
    public List<String> parameters;

    public ErrorRecord error;

    /**
     * List of block evaluations that happened on this code attribute.
     */
    public List<InstructionBlockEvaluationRecord> blockEvaluations = new ArrayList<>();

    public CodeAttributeRecord(String clazz, String method, List<String> parameters, List<InstructionRecord> instructions)
    {
        this.clazz = clazz;
        this.method = method;
        this.parameters = parameters;
        this.instructions = instructions;
    }
}