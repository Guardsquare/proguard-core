package proguard.evaluation.stateTrackers.machinePrinter;

import org.jetbrains.annotations.NotNull;

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
    @NotNull
    private final String clazz;

    /**
     * Method this code attribute is from.
     */
    @NotNull
    private final String method;

    /**
     * List of instruction from this code attribute.
     */
    @NotNull
    private final List<InstructionRecord> instructions;

    /**
     * List of parameters given to the code attribute.
     */
    @NotNull
    private final List<String> parameters;

    private ErrorRecord error;

    /**
     * List of block evaluations that happened on this code attribute.
     */
    @NotNull
    private final List<InstructionBlockEvaluationRecord> blockEvaluations = new ArrayList<>();

    public CodeAttributeRecord(@NotNull String clazz, @NotNull String method, @NotNull List<String> parameters,
                               @NotNull List<InstructionRecord> instructions)
    {
        this.clazz = clazz;
        this.method = method;
        this.parameters = parameters;
        this.instructions = instructions;
    }

    @NotNull
    public String getClazz()
    {
        return clazz;
    }

    @NotNull
    public String getMethod()
    {
        return method;
    }

    @NotNull
    public List<InstructionRecord> getInstructions()
    {
        return instructions;
    }

    @NotNull
    public List<String> getParameters()
    {
        return parameters;
    }

    public ErrorRecord getError()
    {
        return error;
    }

    @NotNull
    public List<InstructionBlockEvaluationRecord> getBlockEvaluations()
    {
        return blockEvaluations;
    }

    public void setError(ErrorRecord error)
    {
        this.error = error;
    }
}
