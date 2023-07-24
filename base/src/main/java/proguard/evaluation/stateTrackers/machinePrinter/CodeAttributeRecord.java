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
    private final String clazz;

    /**
     * Method this code attribute is from.
     */
    private final String method;

    /**
     * List of instruction from this code attribute.
     */
    private final List<InstructionRecord> instructions;

    /**
     * List of parameters given to the code attribute.
     */
    private final List<String> parameters;

    private ErrorRecord error;

    /**
     * List of block evaluations that happened on this code attribute.
     */
    private final List<InstructionBlockEvaluationRecord> blockEvaluations = new ArrayList<>();

    public CodeAttributeRecord(String clazz, String method, List<String> parameters, List<InstructionRecord> instructions)
    {
        this.clazz = clazz;
        this.method = method;
        this.parameters = parameters;
        this.instructions = instructions;
    }

    public String getClazz()
    {
        return clazz;
    }

    public String getMethod()
    {
        return method;
    }

    public List<InstructionRecord> getInstructions()
    {
        return instructions;
    }

    public List<String> getParameters()
    {
        return parameters;
    }

    public ErrorRecord getError()
    {
        return error;
    }

    public List<InstructionBlockEvaluationRecord> getBlockEvaluations()
    {
        return blockEvaluations;
    }

    public void setError(ErrorRecord error)
    {
        this.error = error;
    }
}
